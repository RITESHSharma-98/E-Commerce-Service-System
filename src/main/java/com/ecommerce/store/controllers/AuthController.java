package com.ecommerce.store.controllers;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;

import com.google.api.client.json.jackson2.JacksonFactory;
import com.ecommerce.store.dtos.JwtRequest;
import com.ecommerce.store.dtos.JwtResponse;
import com.ecommerce.store.dtos.UserDto;
import com.ecommerce.store.entities.User;
import com.ecommerce.store.exceptions.BadApiRequestException;
import com.ecommerce.store.security.JwtHelper;
import com.ecommerce.store.services.UserService;
import com.ecommerce.store.dtos.ApiResponseMessage;
import com.ecommerce.store.services.EmailService;
import com.ecommerce.store.services.OtpService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@Api(value = "AuthController", tags = "Authentication APIs")
// @CrossOrigin(
// origins = "http://localhost:4200",
// allowedHeaders = {"Authorization"},
// methods = {RequestMethod.GET,RequestMethod.POST},
// maxAge = 3600
// )
public class AuthController {

    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private AuthenticationManager manager;
    @Autowired
    private UserService userService;

    @Autowired
    private JwtHelper helper;

    @Value("${googleClientId}")
    private String googleClientId;
    @Value("${newPassword}")
    private String newPassword;

    @Autowired
    private OtpService otpService;

    @Autowired
    private EmailService emailService;

    private Logger logger = LoggerFactory.getLogger(AuthController.class);

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(@RequestBody JwtRequest request) {
        this.doAuthenticate(request.getEmail(), request.getPassword());
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = this.helper.generateToken(userDetails);
        UserDto userDto = modelMapper.map(userDetails, UserDto.class);
        JwtResponse response = JwtResponse.builder()
                .jwtToken(token)
                .user(userDto).build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private void doAuthenticate(String email, String password) {

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(email, password);
        try {
            manager.authenticate(authentication);
        } catch (BadCredentialsException e) {
            throw new BadApiRequestException(" Invalid Username or Password  !!");
        }

    }

    @GetMapping("/current")
    public ResponseEntity<UserDto> getCurrentUser(Principal principal) {
        if (principal == null) {
            throw new BadApiRequestException("User is not authenticated !!");
        }
        String name = principal.getName();
        return new ResponseEntity<>(modelMapper.map(userDetailsService.loadUserByUsername(name), UserDto.class),
                HttpStatus.OK);
    }

    // login with google api

    @PostMapping("/google")
    public ResponseEntity<JwtResponse> loginWithGoogle(@RequestBody Map<String, Object> data) throws IOException {

        // get the id token from request
        String idToken = data.get("idToken").toString();

        NetHttpTransport netHttpTransport = new NetHttpTransport();

        JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();

        GoogleIdTokenVerifier.Builder verifier = new GoogleIdTokenVerifier.Builder(netHttpTransport, jacksonFactory)
                .setAudience(Collections.singleton(googleClientId));

        GoogleIdToken googleIdToken = GoogleIdToken.parse(verifier.getJsonFactory(), idToken);

        GoogleIdToken.Payload payload = googleIdToken.getPayload();

        logger.info("Payload : {}", payload);

        String email = payload.getEmail();

        User user = null;

        user = userService.findUserByEmailOptional(email).orElse(null);

        if (user == null) {
            // create new user
            user = this.saveUser(email, data.get("name").toString(), data.get("photoUrl").toString());
        } else {
            // Sync password if user already exists (for existing Google users)
            UserDto userDto = modelMapper.map(user, UserDto.class);
            userDto.setPassword(newPassword);
            userService.updateUser(userDto, user.getUserId());
        }
        ResponseEntity<JwtResponse> jwtResponseResponseEntity = this
                .login(JwtRequest.builder().email(user.getEmail()).password(newPassword).build());
        return jwtResponseResponseEntity;

    }

    @PostMapping("/send-otp")
    @ApiOperation(value = "Send OTP to email for password reset !!")
    public ResponseEntity<ApiResponseMessage> sendOtp(@RequestParam String email) {
        // Check if user exists
        userService.getUserByEmail(email);

        String otp = otpService.generateOtp(email);
        String subject = "OTP for Password Reset - Electronic Store";
        String message = "Your OTP for password reset is: " + otp + "\n\nThis OTP is valid for 5 minutes.";

        emailService.sendEmail(email, subject, message);

        ApiResponseMessage response = ApiResponseMessage.builder()
                .message("OTP sent successfully to " + email)
                .success(true)
                .status(HttpStatus.OK)
                .build();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/reset-password")
    @ApiOperation(value = "Verify OTP and reset password !!")
    public ResponseEntity<ApiResponseMessage> resetPassword(
            @RequestParam String email,
            @RequestParam String otp,
            @RequestParam String newPassword) {

        boolean isVerified = otpService.verifyOtp(email, otp);
        if (isVerified) {
            UserDto userDto = userService.getUserByEmail(email);
            userDto.setPassword(newPassword); // UserController/UserServiceImpl will encode it
            userService.updateUser(userDto, userDto.getUserId());

            ApiResponseMessage response = ApiResponseMessage.builder()
                    .message("Password reset successfully !!")
                    .success(true)
                    .status(HttpStatus.OK)
                    .build();
            return new ResponseEntity<>(response, HttpStatus.OK);
        } else {
            ApiResponseMessage response = ApiResponseMessage.builder()
                    .message("Invalid or expired OTP !!")
                    .success(false)
                    .status(HttpStatus.BAD_REQUEST)
                    .build();
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    private User saveUser(String email, String name, String photoUrl) {

        UserDto newUser = UserDto.builder()
                .name(name)
                .email(email)
                .password(newPassword)
                .imageName(photoUrl)
                .roles(new HashSet<>())
                .build();

        UserDto user = userService.createUser(newUser);

        return this.modelMapper.map(user, User.class);

    }

}
