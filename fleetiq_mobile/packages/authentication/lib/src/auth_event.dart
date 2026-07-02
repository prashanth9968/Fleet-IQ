abstract class AuthEvent {}

class AuthLoginRequested extends AuthEvent {
  final String email;
  final String password;
  final String tenantSlug;

  AuthLoginRequested({
    required this.email,
    required this.password,
    required this.tenantSlug,
  });
}

class AuthLogoutRequested extends AuthEvent {}

class AuthCheckSession extends AuthEvent {}

class AuthTokenRefreshRequested extends AuthEvent {}
