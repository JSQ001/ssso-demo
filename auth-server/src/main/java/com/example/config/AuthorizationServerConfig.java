package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;
import org.springframework.security.oauth2.provider.token.TokenEnhancerChain;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.redis.RedisTokenStore;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.util.Arrays;

/**
 * 授权服务器配置，主要令牌路径的安全性，客户端详情和令牌存储
 */
@Configuration
@EnableAuthorizationServer
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    @Resource
    private DataSource dataSource;

    @Resource
    private CustomTokenEnhancer customTokenEnhancer;

    /**
     * 配置授权服务器的安全性，令牌端点的安全约束
     *
     * @param security
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
        security
                // 开启 /oauth/check_token
                .tokenKeyAccess("permitAll()")
                // 开启 /oauth/token_key
                .checkTokenAccess("isAuthenticated()")
                // 允许表单认证，如果配置支持allowFormAuthenticationForClients的，且url中有client_id和client_secret的会走ClientCredentialsTokenEndpointFilter来保护
                // 如果没有支持allowFormAuthenticationForClients或者有支持，但是url中没有client_id和client_secret的，走basic认证保护
                .allowFormAuthenticationForClients();
    }

    /**
     *  配置客户端，可存在内存和数据库中
     *
     * @param clients
     * @throws Exception
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        clients
            .withClientDetails(clientDetails(dataSource));
    }

    /**
     *
     * 配置授权服务器端点的非安全功能，例如令牌存储，令牌自定义，用户批准和授予类型
     *
     * @param endpoints
     * @throws Exception
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
        endpoints
                .tokenServices(tokenService())
                .tokenStore(tokenStore())
                //.tokenEnhancer(tokenEnhancerChain())
                // 密码授权方式时需要
                .authenticationManager(authenticationManager)
                // /oauth/token 运行get和post
                .allowedTokenEndpointRequestMethods(HttpMethod.GET, HttpMethod.POST);
    }

    /**
     * token 令牌服务
     * @return
     */
    @Bean
    public AuthorizationServerTokenServices tokenService() {
        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
        defaultTokenServices.setTokenStore(tokenStore());  //关联存储方式
        defaultTokenServices.setTokenEnhancer(tokenEnhancerChain());
        defaultTokenServices.setSupportRefreshToken(true);
        defaultTokenServices.setAccessTokenValiditySeconds(7200); //令牌有效期 两小时
        defaultTokenServices.setRefreshTokenValiditySeconds(259200); //刷新令牌有效期 三天
        return defaultTokenServices;
    }

    /**
     * 配置token增强
     *
     */
    @Bean
    public TokenEnhancerChain tokenEnhancerChain(){
        // token增强配置
        TokenEnhancerChain tokenEnhancerChain = new TokenEnhancerChain();
        tokenEnhancerChain.setTokenEnhancers(Arrays.asList(customTokenEnhancer, jwtAccessTokenConverter()));
        return tokenEnhancerChain;
    }

    /**
     * 配置redis，使用redis存token
     * @return
     */
    @Bean
    public TokenStore tokenStore(){
        return new RedisTokenStore(redisConnectionFactory);
    }

    /**
     * 获取客户端详细信息服务，JDBC实现
     * @return
     */
    @Bean
    public ClientDetailsService clientDetails(DataSource dataSource) {
        return new JdbcClientDetailsService(dataSource);
    }

    /**
     * 用来生成token的转换器
     * @return
     */
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter() {
        JwtAccessTokenConverter jwtAccessTokenConverter = new JwtAccessTokenConverter();
        // 对称加密，设置签名，使用下面这个值作为密钥 tokenKey
        jwtAccessTokenConverter.setSigningKey("oauth");
        return jwtAccessTokenConverter;
    }
}
