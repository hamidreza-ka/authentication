package com.my.authentication.registration;

import com.my.authentication.appuser.AppUser;
import com.my.authentication.appuser.AppUserRole;
import com.my.authentication.appuser.AppUserService;
import com.my.authentication.email.EmailSender;
import com.my.authentication.exception.MyException;
import com.my.authentication.jwt.JwtResponse;
import com.my.authentication.jwt.JwtUtil;
import com.my.authentication.registration.blacklist.BlackListTokenService;
import com.my.authentication.registration.token.ConfirmationToken;
import com.my.authentication.registration.token.ConfirmationTokenService;
import io.jsonwebtoken.Claims;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;

@Service
@AllArgsConstructor
public class RegistrationService {

    private final EmailValidator emailValidator;
    private final AppUserService appUserService;
    private final ConfirmationTokenService confirmationTokenService;
    private final EmailSender emailSender;
    private final JwtUtil jwtUtil;
    private final BlackListTokenService blackListTokenService;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public String register(RegistrationRequest request) {
        boolean isValidEmail = emailValidator.test(request.getEmail());

        if (!isValidEmail)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "email isn't valid");

        String token;

        if (appUserService.findByEmail(request.getEmail()).isPresent())
            token = appUserService.signUpUser(appUserService.findByEmail(request.getEmail()).get());
        else
            token = appUserService.signUpUser(new AppUser(request.getFirst_name(), request.getLast_name(), request.getEmail(),
                    "", request.getPassword(), AppUserRole.USER));

        String link = "http://localhost:8085/api/v1/registration/confirm?token=" + token;
        //   emailSender.send(request.getEmail(), buildEmail(request.getFirst_name(), link));
        return token;
    }

    public String renewToken(String email) {
        return appUserService.signUpUser((AppUser) appUserService.loadUserByUsername(email));
    }

    @Transactional
    public JwtResponse confirmToken(String token) {
        ConfirmationToken confirmationToken = confirmationTokenService.getToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "code is incorrect"));

        AppUser user = confirmationToken.getAppUser();

        if (confirmationToken.getConfirmedAt() != null) {
            confirmationTokenService.deleteConfirmationToken(token);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "email already confirmed");
        }

        LocalDateTime expiredAt = confirmationToken.getExpiresAt();

        if (expiredAt.isBefore(LocalDateTime.now())) {
            confirmationTokenService.deleteConfirmationToken(token);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "code expired");
        }

        confirmationTokenService.setConfirmedAt(token);
        appUserService.enableAppUser(confirmationToken.getAppUser().getUsername());

        return createJwtToken(user);
    }

    private String buildEmail(String name, String link) {
        return "<div style=\"font-family:Helvetica,Arial,sans-serif;font-size:16px;margin:0;color:#0b0c0c\">\n" +
                "\n" +
                "<span style=\"display:none;font-size:1px;color:#fff;max-height:0\"></span>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;min-width:100%;width:100%!important\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"100%\" height=\"53\" bgcolor=\"#0b0c0c\">\n" +
                "        \n" +
                "        <table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;max-width:580px\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" align=\"center\">\n" +
                "          <tbody><tr>\n" +
                "            <td width=\"70\" bgcolor=\"#0b0c0c\" valign=\"middle\">\n" +
                "                <table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td style=\"padding-left:10px\">\n" +
                "                  \n" +
                "                    </td>\n" +
                "                    <td style=\"font-size:28px;line-height:1.315789474;Margin-top:4px;padding-left:10px\">\n" +
                "                      <span style=\"font-family:Helvetica,Arial,sans-serif;font-weight:700;color:#ffffff;text-decoration:none;vertical-align:top;display:inline-block\">Confirm your email</span>\n" +
                "                    </td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "              </a>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td width=\"10\" height=\"10\" valign=\"middle\"></td>\n" +
                "      <td>\n" +
                "        \n" +
                "                <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse\">\n" +
                "                  <tbody><tr>\n" +
                "                    <td bgcolor=\"#1D70B8\" width=\"100%\" height=\"10\"></td>\n" +
                "                  </tr>\n" +
                "                </tbody></table>\n" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\" height=\"10\"></td>\n" +
                "    </tr>\n" +
                "  </tbody></table>\n" +
                "\n" +
                "\n" +
                "\n" +
                "  <table role=\"presentation\" class=\"m_-6186904992287805515content\" align=\"center\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"border-collapse:collapse;max-width:580px;width:100%!important\" width=\"100%\">\n" +
                "    <tbody><tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "      <td style=\"font-family:Helvetica,Arial,sans-serif;font-size:19px;line-height:1.315789474;max-width:560px\">\n" +
                "        \n" +
                "            <p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\">Hi " + name + ",</p><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> Thank you for registering. Please click on the below link to activate your account: </p><blockquote style=\"Margin:0 0 20px 0;border-left:10px solid #b1b4b6;padding:15px 0 0.1px 15px;font-size:19px;line-height:25px\"><p style=\"Margin:0 0 20px 0;font-size:19px;line-height:25px;color:#0b0c0c\"> <a href=\"" + link + "\">Activate Now</a> </p></blockquote>\n Link will expire in 15 minutes. <p>See you soon</p>" +
                "        \n" +
                "      </td>\n" +
                "      <td width=\"10\" valign=\"middle\"><br></td>\n" +
                "    </tr>\n" +
                "    <tr>\n" +
                "      <td height=\"30\"><br></td>\n" +
                "    </tr>\n" +
                "  </tbody></table><div class=\"yj6qo\"></div><div class=\"adL\">\n" +
                "\n" +
                "</div></div>";
    }

    @Transactional
    public JwtResponse confirmRefreshToken(String token) {

        String tokenId = jwtUtil.extractTokenId(token);
        AppUser user = (AppUser) appUserService.loadUserByUsername(jwtUtil.extractUsername(token));

        boolean tokenIsBlackList = blackListTokenService.searchInBlackList(tokenId, user);
        Date date = jwtUtil.extractExpiration(token);

        if (!jwtUtil.extractGrantType(token).equals("refresh_token") || tokenIsBlackList || date.before(new Date()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh Token is invalid");

//        if ()
//            throw new IllegalStateException("token expired");

        blackListTokenService.addToBlackList(tokenId, user);

        return createJwtToken(user);
    }

    @Transactional
    public JwtResponse login(String userName, String password) {
        AppUser user = appUserService.findByEmail(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "the user credentials were incorrect"));

        if (!bCryptPasswordEncoder.matches(password, user.getPassword()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "the user credentials were incorrect");

        return createJwtToken(user);
    }

    private JwtResponse createJwtToken(AppUser user) {

        String accessToken = jwtUtil.generateToken(user, false);
        String refreshToken = jwtUtil.generateToken(user, true);

        return new JwtResponse("Bearer ",
                String.valueOf(jwtUtil.extractExpiration(accessToken).getTime()),
                accessToken,
                refreshToken);
    }
}
