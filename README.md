# OAuth2.0 on SpringSecurity

# spring boot OAuth2.0 인증 흐름

### oauth2-client가 appliation.property에 등록된 registration과 provider를 참조하여 인증 후 엑세스 토큰을 발급 받음

1. Security가 사용자를 로그인 페이지인 /oauth2/authorization/{provider}로 리다이렉션 시킴
2. 사용자가 로그인하면 Authorization Code를 포함하여 클라이언트가 등록한 Callback Url로 리다이렉션
3. Spring Security의 `OAuth2LoginAuthenticationFilter`가 리다이렉트 url을 가로채어, Authorization Code를 감지
4. `OAuth2AuthorizationCodeGrantRequestEntityConverter`가Authorization Code 포함하여access_token요청을 생성
5. `OAuth2AccessTokenResponseHttpMessageConverter`가 JSON 응답에서 access_token필드를 추출

### 토큰 저장

1. Access Token과 Refresh Token을 `OAuth2AuthorizedClientService`에 `OAuth2AuthorizedClient` 객체로 저장
2. `OAuth2AuthorizedClientRepository`가 이 정보를 보관하여 이후 API 요청에서 사용할 수 있도록 함

### 엑세스 토큰 조회

1. `OAuth2AuthorizedClientService`에서 가져오기

```java
java
@Autowired
private OAuth2AuthorizedClientService authorizedClientService;

public String getAccessToken(OAuth2AuthenticationToken authentication) {
    OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
        authentication.getAuthorizedClientRegistrationId(),
        authentication.getName()
    );
    return client.getAccessToken().getTokenValue();
}

```

- `authorizedClientService.loadAuthorizedClient(clientRegistrationId, principalName)`를 사용해서 특정 사용자의 액세스 토큰을 조회할 수 있음.
- 기본 구현체로는 `InMemoryOAuth2AuthorizedClientService` 와`JdbcOAuth2AuthorizedClientService`가 있음

2. `OAuth2AuthorizedClientRepository` 사용 (Request 스코프에서)

```java

@Autowired
private OAuth2AuthorizedClientRepository authorizedClientRepository;

public String getAccessToken(HttpServletRequest request, Authentication authentication) {
    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withPrincipal(authentication)
        .principal(authentication)
        .build();

    OAuth2AuthorizedClient authorizedClient = authorizedClientRepository.loadAuthorizedClient(
        authentication.getAuthorities().iterator().next().getAuthority(),
        authentication,
        request
    );

    return authorizedClient.getAccessToken().getTokenValue();
}

```

- 요청(`HttpServletRequest`)이 있는 컨텍스트에서 `OAuth2AuthorizedClientRepository`를 통해 액세스 토큰을 가져올 수 있음.

### 사용자 정보 저장

1. 액세스 토큰을 포함한 `OAuth2UserRequest` 생성
2. `Oauth2UserService`(인터페이스)는 `OAuth2UserRequest`를 받아 사용자 정보를 조회(기본은 `DefaultOAuth2UserService`구현체를 사용하지만, custom시 securityConfig에 등록) 
3. 가져온 정보를 기반으로 `Oauth2User`객체를 생성 후 security에 반환
4. Security는 `Oauth2User` 객체와 사용자의 모든 정보를 포함한 `Authentication`(Authentication는 인터페이스, 구현체는 `OAuth2AuthenticationToken`)객체를 생성  후 `SecurityContext`에 저장(이후 `SercurityContextHolder`를 통해 접근 가능)을 통해 인증 정보를 추적

### 사용자 정보 호출

1. SecurityContextHolder.getContext()를 통해서 사용자 정보에 접근

```java
SecurityContext securityContext = SecurityContextHolder.getContext();
Authentication authentication = securityContext.getAuthentication();

if (authentication != null && authentication.isAuthenticated()) {
	OAuth2User oauth2user = (OAuth2User) authentication.getPrincipal();
	
	Map<String, Object> attributes = oauth2User.getAttributes();
	System.out.println("이름: " + attributes.get("displayName"));
	System.out.println("이메일: " + attributes.get("email"));
} else {
// 인증되지 않은 사용자 처리
}

```

1. 컨트롤에서 주입 시 @AuthenticationPrincipal CustomOAuth2User customUser 사용

```java

@GetMapping("/user")
public String getUserInfo(@AuthenticationPrincipal OAuth2User oauth2User) {
    return "User: " + oauth2User.getAttribute("email");
}

```

1. OAuth2AuthenticationToken 사용

```java
java
Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
if (authentication instanceof OAuth2AuthenticationToken) {
    OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
    OAuth2User oauthUser = oauthToken.getPrincipal();
    System.out.println("OAuth User Email: " + oauthUser.getAttribute("email"));
}

```

### **특정 API에 인증된 사용자만 접근 허용**

```java

@PreAuthorize("isAuthenticated()") // 또는 @Secured
@GetMapping("/secure-data")
public String getSecureData() {
    return "Only authenticated users can access this";
}
```
