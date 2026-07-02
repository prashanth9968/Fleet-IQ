import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:fleetiq_theme/fleetiq_theme.dart';
import 'package:fleetiq_authentication/fleetiq_authentication.dart';
import 'package:fleetiq_notifications/fleetiq_notifications.dart';
import 'screens/driver_login_screen.dart';
import 'screens/today_trips_screen.dart';
import 'screens/safety_score_screen.dart';
import 'screens/inspection_screen.dart';
import 'screens/sos_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final prefs = await SharedPreferences.getInstance();
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));

  final authRepository = AuthRepository(dio: dio, prefs: prefs);
  final notificationService = NotificationService();
  await notificationService.initialize();

  runApp(DriverApp(
    authRepository: authRepository,
    notificationService: notificationService,
  ));
}

class DriverApp extends StatelessWidget {
  final AuthRepository authRepository;
  final NotificationService notificationService;

  const DriverApp({
    super.key,
    required this.authRepository,
    required this.notificationService,
  });

  @override
  Widget build(BuildContext context) {
    return MultiBlocProvider(
      providers: [
        BlocProvider(
          create: (_) => AuthBloc(authRepository: authRepository)
            ..add(AuthCheckSession()),
        ),
      ],
      child: MaterialApp(
        title: 'FleetIQ Driver',
        debugShowCheckedModeBanner: false,
        theme: FleetIQTheme.lightTheme,
        darkTheme: FleetIQTheme.darkTheme,
        themeMode: ThemeMode.system,
        home: BlocBuilder<AuthBloc, AuthState>(
          builder: (context, state) {
            if (state is AuthAuthenticated) {
              return const DriverShell();
            }
            return const DriverLoginScreen();
          },
        ),
      ),
    );
  }
}

class DriverShell extends StatefulWidget {
  const DriverShell({super.key});

  @override
  State<DriverShell> createState() => _DriverShellState();
}

class _DriverShellState extends State<DriverShell> {
  int _currentIndex = 0;

  final _screens = const [
    TodayTripsScreen(),
    SafetyScoreScreen(),
    InspectionScreen(),
    SosScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AnimatedSwitcher(
        duration: const Duration(milliseconds: 300),
        child: _screens[_currentIndex],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentIndex,
        onDestinationSelected: (index) => setState(() => _currentIndex = index),
        animationDuration: const Duration(milliseconds: 400),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.route_rounded), label: 'Trips'),
          NavigationDestination(icon: Icon(Icons.shield_rounded), label: 'Safety'),
          NavigationDestination(icon: Icon(Icons.checklist_rounded), label: 'Inspect'),
          NavigationDestination(icon: Icon(Icons.sos_rounded), label: 'SOS'),
        ],
      ),
    );
  }
}
