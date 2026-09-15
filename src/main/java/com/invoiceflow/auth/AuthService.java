package com.invoiceflow.auth;

import com.invoiceflow.organization.Membership;
import com.invoiceflow.organization.MembershipId;
import com.invoiceflow.organization.MembershipRepository;
import com.invoiceflow.organization.Organization;
import com.invoiceflow.organization.OrganizationRepository;
import com.invoiceflow.organization.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResult register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyRegisteredException(request.email());
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .build();
        user = userRepository.save(user);

        Organization organization = Organization.builder()
                .name(request.organizationName())
                .build();
        organization = organizationRepository.save(organization);

        Membership membership = Membership.builder()
                .id(new MembershipId(user.getId(), organization.getId()))
                .user(user)
                .organization(organization)
                .role(Role.OWNER)
                .build();
        membershipRepository.save(membership);

        return issueTokens(user, organization, Role.OWNER);
    }

    @Transactional
    public AuthResult login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // For now, log in to the first organization the user belongs to.
        // Once multi-org switching is built, this becomes an explicit choice.
        Membership membership = membershipRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException("No organization found for this account"));

        return issueTokens(user, membership.getOrganization(), membership.getRole());
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken)
                .orElseThrow(() -> new InvalidCredentialsException("Refresh token is invalid or expired"));

        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new InvalidCredentialsException("User no longer exists"));

        Membership membership = membershipRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException("No organization found for this account"));

        String accessToken = jwtService.generateAccessToken(user);

        return new AuthResult(
                accessToken,
                rotation.newRawToken(),
                user,
                membership.getOrganization(),
                membership.getRole()
        );
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenService.revokeAllForUser(userId);
    }

    private AuthResult issueTokens(User user, Organization organization, Role role) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = refreshTokenService.issue(user.getId());
        return new AuthResult(accessToken, refreshToken, user, organization, role);
    }

    /** Carries both tokens plus enough context to build the AuthResponse and set the cookie. */
    public record AuthResult(
            String accessToken,
            String refreshToken,
            User user,
            Organization organization,
            Role role
    ) {
    }
}
