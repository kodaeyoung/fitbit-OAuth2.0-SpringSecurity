package com.FitbitOauthOnSecurity.fitbit;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class FitbitData {

    @Autowired
    private OAuth2AuthorizedClientService authorizedClientService;
    @Autowired
    private WebClient webClient;

    //토큰 반환
    public String getAccessToken(OAuth2AuthenticationToken authentication) {
        OAuth2AuthorizedClient authorizedClient = this.authorizedClientService.loadAuthorizedClient(
                authentication.getAuthorizedClientRegistrationId(), authentication.getName());

        return authorizedClient.getAccessToken().getTokenValue();
    }

    //사용자 정보 반환
    public Map<String, Object> getUserInfo(OAuth2AuthenticationToken authentication){
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        return oAuth2User.getAttributes();  // 사용자 정보 반환
    }

    @GetMapping("/req")
    public Mono<String> getActivities() {
        OAuth2AuthenticationToken authentication = (OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        String date = "2024-08-01";
        String accessToken = getAccessToken(authentication);
        return webClient.get()
                .uri("https://api.fitbit.com/1/user/-/activities/date/" + date + ".json")
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(String.class);
    }
}
