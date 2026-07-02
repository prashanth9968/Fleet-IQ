import 'package:flutter_bloc/flutter_bloc.dart';
import 'auth_event.dart';
import 'auth_state.dart';
import 'auth_repository.dart';

class AuthBloc extends Bloc<AuthEvent, AuthState> {
  final AuthRepository authRepository;

  AuthBloc({required this.authRepository}) : super(AuthInitial()) {
    on<AuthCheckSession>(_onCheckSession);
    on<AuthLoginRequested>(_onLogin);
    on<AuthLogoutRequested>(_onLogout);
    on<AuthTokenRefreshRequested>(_onRefreshToken);
  }

  Future<void> _onCheckSession(AuthCheckSession event, Emitter<AuthState> emit) async {
    if (authRepository.isAuthenticated) {
      emit(AuthAuthenticated(
        email: authRepository.currentUserEmail ?? '',
        tenantId: authRepository.currentTenantId ?? '',
        token: authRepository.currentToken!,
      ));
    } else {
      emit(AuthUnauthenticated());
    }
  }

  Future<void> _onLogin(AuthLoginRequested event, Emitter<AuthState> emit) async {
    emit(AuthLoading());
    try {
      final data = await authRepository.login(event.email, event.password, event.tenantSlug);
      emit(AuthAuthenticated(
        email: event.email,
        tenantId: data['tenantId'] as String? ?? '',
        token: data['token'] as String,
      ));
    } catch (e) {
      emit(AuthError(e.toString()));
    }
  }

  Future<void> _onLogout(AuthLogoutRequested event, Emitter<AuthState> emit) async {
    await authRepository.logout();
    emit(AuthUnauthenticated());
  }

  Future<void> _onRefreshToken(AuthTokenRefreshRequested event, Emitter<AuthState> emit) async {
    final newToken = await authRepository.refreshToken();
    if (newToken != null) {
      emit(AuthAuthenticated(
        email: authRepository.currentUserEmail ?? '',
        tenantId: authRepository.currentTenantId ?? '',
        token: newToken,
      ));
    } else {
      emit(AuthUnauthenticated());
    }
  }
}
