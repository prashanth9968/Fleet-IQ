import 'package:flutter/material.dart';

class FleetIQTheme {
  // Brand Color Palette
  static const Color primaryIndigo = Color(0xFF4F46E5);
  static const Color secondarySlate = Color(0xFF64748B);
  static const Color backgroundSlate = Color(0xFFF8FAFC);
  static const Color surfaceSlate = Color(0xFFFFFFFF);
  static const Color errorRed = Color(0xFFEF4444);
  static const Color warningAmber = Color(0xFFF59E0B);
  static const Color successGreen = Color(0xFF10B981);

  static const Color darkPrimaryIndigo = Color(0xFF818CF8);
  static const Color darkSecondarySlate = Color(0xFF94A3B8);
  static const Color darkBackgroundSlate = Color(0xFF0F172A);
  static const Color darkSurfaceSlate = Color(0xFF1E293B);

  static ThemeData get lightTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.light,
      colorScheme: const ColorScheme.light(
        primary: primaryIndigo,
        onPrimary: Colors.white,
        secondary: secondarySlate,
        onSecondary: Colors.white,
        error: errorRed,
        onError: Colors.white,
        background: backgroundSlate,
        onBackground: Color(0xFF0F172A),
        surface: surfaceSlate,
        onSurface: Color(0xFF0F172A),
        surfaceVariant: Color(0xFFE2E8F0),
        onSurfaceVariant: Color(0xFF475569),
      ),
      cardTheme: CardTheme(
        color: surfaceSlate,
        elevation: 2,
        shadowColor: Colors.black.withOpacity(0.05),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: primaryIndigo,
          foregroundColor: Colors.white,
          elevation: 2,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: primaryIndigo,
          side: const BorderSide(color: primaryIndigo, width: 1.5),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFCBD5E1)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFCBD5E1)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: primaryIndigo, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: errorRed),
        ),
      ),
      textTheme: const TextTheme(
        displayLarge: TextStyle(fontFamily: 'Roboto', fontSize: 32, fontWeight: FontWeight.bold, color: Color(0xFF0F172A)),
        displayMedium: TextStyle(fontFamily: 'Roboto', fontSize: 28, fontWeight: FontWeight.bold, color: Color(0xFF0F172A)),
        displaySmall: TextStyle(fontFamily: 'Roboto', fontSize: 24, fontWeight: FontWeight.bold, color: Color(0xFF0F172A)),
        headlineLarge: TextStyle(fontFamily: 'Roboto', fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFF0F172A)),
        headlineMedium: TextStyle(fontFamily: 'Roboto', fontSize: 20, fontWeight: FontWeight.w600, color: Color(0xFF0F172A)),
        titleLarge: TextStyle(fontFamily: 'Roboto', fontSize: 18, fontWeight: FontWeight.w600, color: Color(0xFF0F172A)),
        titleMedium: TextStyle(fontFamily: 'Roboto', fontSize: 16, fontWeight: FontWeight.w500, color: Color(0xFF0F172A)),
        bodyLarge: TextStyle(fontFamily: 'Roboto', fontSize: 16, color: Color(0xFF334155)),
        bodyMedium: TextStyle(fontFamily: 'Roboto', fontSize: 14, color: Color(0xFF475569)),
        bodySmall: TextStyle(fontFamily: 'Roboto', fontSize: 12, color: Color(0xFF64748B)),
      ),
    );
  }

  static ThemeData get darkTheme {
    return ThemeData(
      useMaterial3: true,
      brightness: Brightness.dark,
      colorScheme: const ColorScheme.dark(
        primary: darkPrimaryIndigo,
        onPrimary: Color(0xFF0F172A),
        secondary: darkSecondarySlate,
        onSecondary: Color(0xFF0F172A),
        error: errorRed,
        onError: Colors.black,
        background: darkBackgroundSlate,
        onBackground: Color(0xFFF8FAFC),
        surface: darkSurfaceSlate,
        onSurface: Color(0xFFF8FAFC),
        surfaceVariant: Color(0xFF334155),
        onSurfaceVariant: Color(0xFF94A3B8),
      ),
      cardTheme: CardTheme(
        color: darkSurfaceSlate,
        elevation: 4,
        shadowColor: Colors.black.withOpacity(0.2),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(16),
        ),
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: darkPrimaryIndigo,
          foregroundColor: const Color(0xFF0F172A),
          elevation: 2,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
      outlinedButtonTheme: OutlinedButtonThemeData(
        style: OutlinedButton.styleFrom(
          foregroundColor: darkPrimaryIndigo,
          side: const BorderSide(color: darkPrimaryIndigo, width: 1.5),
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 16),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
          ),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: darkSurfaceSlate,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF475569)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFF475569)),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: darkPrimaryIndigo, width: 2),
        ),
        errorBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: errorRed),
        ),
      ),
      textTheme: const TextTheme(
        displayLarge: TextStyle(fontFamily: 'Roboto', fontSize: 32, fontWeight: FontWeight.bold, color: Color(0xFFF8FAFC)),
        displayMedium: TextStyle(fontFamily: 'Roboto', fontSize: 28, fontWeight: FontWeight.bold, color: Color(0xFFF8FAFC)),
        displaySmall: TextStyle(fontFamily: 'Roboto', fontSize: 24, fontWeight: FontWeight.bold, color: Color(0xFFF8FAFC)),
        headlineLarge: TextStyle(fontFamily: 'Roboto', fontSize: 22, fontWeight: FontWeight.bold, color: Color(0xFFF8FAFC)),
        headlineMedium: TextStyle(fontFamily: 'Roboto', fontSize: 20, fontWeight: FontWeight.w600, color: Color(0xFFF8FAFC)),
        titleLarge: TextStyle(fontFamily: 'Roboto', fontSize: 18, fontWeight: FontWeight.w600, color: Color(0xFFF8FAFC)),
        titleMedium: TextStyle(fontFamily: 'Roboto', fontSize: 16, fontWeight: FontWeight.w500, color: Color(0xFFF8FAFC)),
        bodyLarge: TextStyle(fontFamily: 'Roboto', fontSize: 16, color: Color(0xFFE2E8F0)),
        bodyMedium: TextStyle(fontFamily: 'Roboto', fontSize: 14, color: Color(0xFFCBD5E1)),
        bodySmall: TextStyle(fontFamily: 'Roboto', fontSize: 12, color: Color(0xFF94A3B8)),
      ),
    );
  }
}
