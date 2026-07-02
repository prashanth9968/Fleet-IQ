import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:fleetiq_theme/fleetiq_theme.dart';
import 'package:fleetiq_authentication/fleetiq_authentication.dart';
import 'package:fleetiq_notifications/fleetiq_notifications.dart';
import 'screens/login_screen.dart';
import 'screens/dashboard_screen.dart';
import 'screens/vehicle_list_screen.dart';
import 'screens/live_map_screen.dart';
import 'screens/alerts_screen.dart';
import 'screens/settings_screen.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();

  final prefs = await SharedPreferences.getInstance();
  final dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080/api/v1'));

  final authRepository = AuthRepository(dio: dio, prefs: prefs);
  final notificationService = NotificationService();
  await notificationService.initialize();

  runApp(FleetManagerApp(
    authRepository: authRepository,
    notificationService: notificationService,
  ));
}

class FleetManagerApp extends StatelessWidget {
  final AuthRepository authRepository;
  final NotificationService notificationService;

  const FleetManagerApp({
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
        title: 'FleetIQ Manager',
        debugShowCheckedModeBanner: false,
        theme: FleetIQTheme.lightTheme,
        darkTheme: FleetIQTheme.darkTheme,
        themeMode: ThemeMode.system,
        home: BlocBuilder<AuthBloc, AuthState>(
          builder: (context, state) {
            if (state is AuthAuthenticated) {
              return const ManagerShell();
            }
            return const LoginScreen();
          },
        ),
      ),
    );
  }
}

class ManagerShell extends StatefulWidget {
  const ManagerShell({super.key});

  @override
  State<ManagerShell> createState() => _ManagerShellState();
}

class _ManagerShellState extends State<ManagerShell> {
  int _currentIndex = 0;

  final _screens = const [
    DashboardScreen(),
    VehicleListScreen(),
    LiveMapScreen(),
    AlertsScreen(),
    SettingsScreen(),
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
          NavigationDestination(icon: Icon(Icons.dashboard_rounded), label: 'Dashboard'),
          NavigationDestination(icon: Icon(Icons.local_shipping_rounded), label: 'Vehicles'),
          NavigationDestination(icon: Icon(Icons.map_rounded), label: 'Live Map'),
          NavigationDestination(icon: Icon(Icons.notifications_active_rounded), label: 'Alerts'),
          NavigationDestination(icon: Icon(Icons.settings_rounded), label: 'Settings'),
        ],
      ),
    );
  }
}
