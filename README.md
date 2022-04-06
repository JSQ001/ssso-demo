##Spring Security OAuth2对单点登录和登出的实现。

Spring Security OAuth 建立在Spring Security 之上，所以大部分配置还是在Security中，Security完成对用户的认证和授权，OAuth完成单点登录。

### 单点登录流程
####1. 请求授权码，判断未登录，重定向登录页
访问客户端受保护资源 localhost:9001/client1/user，未登录重定向到 localhost:9001/client1/login 进行登录认证，因为配置了单点登录@EnableOAuth2Sso，所以单点登录拦截器会读取授权服务器的配置，发起获取授权码请求
http://localhost:9000/auth-server/oauth/authorize?client_id=client_1&redirect_uri=http://localhost:9001/client1/login&response_type=code&state=code,
被auth-server的 AuthorizationEndpoint.authorize() 处理，因为未登录认证，抛出InsufficientAuthenticationException异常

    if (!(principal instanceof Authentication) || !((Authentication) principal).isAuthenticated()) {
        throw new InsufficientAuthenticationException(
        "User must be authenticated with Spring Security before authorization can be completed.");
    }
异常在 ExceptionTranslationFilter.doFilter() 中处理 `handleSpringSecurityException(request, response, chain, ase);`

调用 LoginUrlAuthenticationEntryPoint.commence() 方法，获取登录页地址，并重定向
`redirectUrl = buildRedirectUrlToLoginPage(request, response, authException);`   

####2. 登录成功，重定向继续请求授权码，未被资源所有者批准，返回批准页面
在auth-server中用户密码由 AbstractAuthenticationProcessingFilter.doFilter() 处理，UsernamePasswordAuthenticationFilter 继承自 AbstractAuthenticationProcessingFilter，在父类 doFilter() 方法中，会调用子类实现的 attemptAuthentication 方法，获取认证信息

    authResult = attemptAuthentication(request, response);

在 attemptAuthentication() 方法中，将用户名和密码封装成token并认证，并添加额外信息后，进行认证

    this.getAuthenticationManager().authenticate(authRequest);

getAuthenticationManager() 方法获取 AuthenticationManager 的实现类 ProviderManager，在 authenticate() 方法中，找到合适的 AuthenticationProvider 处理认证，这里是 DaoAuthenticationProvider，它父类 AbstractUserDetailsAuthenticationProvider 实现了该方法

    result = provider.authenticate(authentication);

父类会调用 retrieveUser() 方法检索用户，实现在 DaoAuthenticationProvider
    
    user = retrieveUser(username,(UsernamePasswordAuthenticationToken) authentication);

这里是从内存或数据库中获取用户，然后进行密码校验，成功后，将信息保存到Authentication，并返回,调用成功Handler。

默认登录成功，会重定向之前请求的地址
http://localhost:9000/auth-server/oauth/authorize?client_id=client_1&redirect_uri=http://localhost:8001/service1/login&response_type=code&state=code,
再次被auth-server的 AuthorizationEndpoint.authorize() 处理，这时有用户认证信息，获取client信息，进行检查，检查资源所有者是否批准（客户端可设置是否自动批准,）如果未批准，返回批准页，请求转发 forward:/oauth/confirm_access

    return getUserApprovalPageResponse(model, authorizationRequest, (Authentication) principal);

####3. 资源所有者批准，重定向返回授权码
用户批准后，被 AuthorizationEndpoint.approveOrDeny() 方法处理，返回授权码，并重定向用户设置的地址(/login)，并带上code和state

    return getAuthorizationCodeResponse(authorizationRequest, (Authentication) principal);

####4. 客户端获取到授权码，请求Token
在客户端 AbstractAuthenticationProcessingFilter 中处理
authResult = attemptAuthentication(request, response);

由子类 OAuth2ClientAuthenticationProcessingFilter.attemptAuthentication() 处理，判断token是否为空
    
    accessToken = restTemplate.getAccessToken();

如果为空，在 AuthorizationCodeAccessTokenProvider.obtainAccessToken() 方法中，获取返回的授权码，向auth-server请求Token

    return retrieveToken(request, resource, getParametersForTokenRequest(resource, request),getHeadersForTokenRequest(request));

在auth-server中 TokenEndpoint.getAccessToken() 方法获取token，进行客户端校验后生成token并返回
   
    OAuth2AccessToken token = getTokenGranter().grant(tokenRequest.getGrantType(), tokenRequest);

####5. 获取到Token，重定向 /user

回到在客户端 OAuth2ClientAuthenticationProcessingFilter.attemptAuthentication() 中，获取到token后，带上token，向auth-server请求用户信息。
默认Token是使用uuid，生成用于认证的token和刷新的Token。认证Token默认12小时过期，刷新的Token默认30天过期。

    OAuth2Authentication result = tokenServices.loadAuthentication(accessToken.getValue());

在auth-server 被 OAuth2AuthenticationProcessingFilter 处理，从头部获取并验证token后，完成该请求。
客户端获取到用户信息，在客户端重新完成登录的流程，最后在默认的登录成功Handler中获取到重定向地址(即 /user)，并重定向。

#### 单点登出
这里除了部分的资源服务器中配置的api需要token验证，其他还是依赖于Spring Security的认证。而Spring Security是使用Cookie和Session的记录用户。所以可以将认证中心和各个子系统的Cookie设置在同一路径下，在认证中心登出时，将Cookie一并删除，实现认证中心和各个子系统的登出。各子系统需要知道认证中心的登出地址。在这里是http://localhost:8000/auth-server/logout。
修改认证中心和各个子系统的Cookie路径
    
    server:
        servlet:
            session:
                cookie:
                    path: /


### 单点登录和登出
单点登录是有多个子系统，一个认证中心。当访问其中任意一个子系统时，如果发现未登录，就跳到认证中心进行登录，登录完成后再跳回该子系统。
此时再访问其他子系统时，就已经是登录状态了。
单点登出是统一从认证中心登出，登出后各个子系统就无法访问了，需要再次登录。
单点登录主要靠@EnableOAuth2Sso实现，简化了从资源服务器到认证授权服务器的SSO流程，并使用授权码方式获取。 配置过长在此省略


### 总结
1. @EnableOAuth2Sso 
会将资源服务器标记为OAuth 2.0的客户端， 它将负责将资源所有者（最终用户）重定向到用户必须输入其凭据的授权服务器。完成后，用户将被重定向回具有授权码的客户端。然后客户端通过调用授权服务器获取授权代码并将其交换为访问令牌。只有在此之后，客户端才能使用访问令牌调用资源服务器。

2. @EnableResourceServer 
意味着所属的服务需要访问令牌才能处理请求。在调用资源服务器之前，需要先从授权服务器获取访问令牌。

3. 在资源服务器中配置的路径，都会被 OAuth2AuthenticationProcessingFilter 处理，获取token。

4. 客户端获取到了token，为什么在访问 /user 的请求头中并没有Authorization，亦可请求成功。其实都因为Security。没有在资源服务器中配置的路径，登录认证成功后并不需要携带token，而还是使用Security需要的Cookie和Session。

5. 使用Jwt增强token，如果资源服务器没有配置tokenService，就会调用配置的userInfoUri去auth-server获取用户信息；如果资源服务器配置了tokenService，再加上有UserDetails的实现类，可以解析，就不用在调用auth-server的接口。
这里service-1配置了tokenService，service-2没有配置tokenService。


