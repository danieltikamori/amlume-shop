# TODO

### Hashicorp Vault

* Set up HashiCorp Vault server

* Configure proper authentication
 
* Set up proper access policies

* Use environment variables for Vault configuration

* Implement proper secret rotation

* Monitor Vault operations

* Set up proper backup procedures

* Implement proper error handling

* Use appropriate logging levels

* Test failure scenarios


## Security

### Authentication

#### User Registration

- [ ] Implement user registration
- [ ] Implement user login
- [ ] Implement user logout
- [ ] Implement user password reset
- [ ] Implement user password change
- [ ] Implement user account locking
- [ ] Implement user account unlocking
- [ ] Implement user account deletion
- [ ] Implement user account suspension
- [ ] Implement user account reactivation
- [ ] Implement user account reset
- [ ] Implement user account password reset
- [ ] Implement user account password change
- [ ] Implement user account password reset

#### User Authentication

- [x] Implement user authentication
- [x] Implement user authorization
- [x] Implement user authentication with MFA
- [x] Implement user authorization with MFA
- [x] Implement user authentication with TOTP
- [x] Implement user authorization with TOTP
- [x] Implement user authentication with MFA and TOTP
- [x] Implement user authorization with MFA and TOTP
- [x] Implement user authentication with MFA and TOTP and password
- [x] Implement user authorization with MFA and TOTP and password

#### User Tokens

- [x] Implement user access tokens
- [x] Implement user refresh tokens
- [ ] Implement user refresh tokens with MFA?
- [ ] Implement user refresh tokens with TOTP?
- [ ] Implement device fingerprinting using separated database table:
- Compare the new fingerprint to any previously recorded fingerprints for that user.

* If it is a match, then proceed to generate new access and refresh tokens.
* If it is not a match, then consider it as a suspicious activity, and you can take a few actions:
- Log the event: Record the mismatch for security auditing.
- Increase scrutiny: Require additional verification steps (e.g., email verification, two-factor authentication) before issuing new tokens.
- Limit refresh token usage: Restrict the number of refresh token requests from different fingerprints within a certain time window.
- Consider revoking the refresh token: This is a more aggressive option.

###### Refresh Token Validation logic:

1. Verify refresh token signature, expiration, JTI.
2. Get user information from database.

3. Generate NEW device fingerprint from current request.

4. Check if the NEW fingerprint is in the user's list of PREVIOUS fingerprints.

5. If it is a match OR the user has no PREVIOUS fingerprints:
   a. Generate new access token.
   b. Generate new refresh token.
   c. Update user's list of fingerprints (add the new fingerprint).
   d. Return new tokens.

6. If it is NOT a match:
   a. Log suspicious activity.
   b. Consider additional verification steps (2FA, email verification).
   c. Potentially revoke the refresh token.
   d. Return an error (e.g., "Suspicious activity detected").
