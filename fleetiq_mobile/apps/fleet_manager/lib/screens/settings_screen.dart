import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:fleetiq_authentication/fleetiq_authentication.dart';

class SettingsScreen extends StatelessWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Settings', style: theme.textTheme.headlineLarge),
            const SizedBox(height: 24),

            // Profile Header
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Row(
                  children: [
                    CircleAvatar(
                      radius: 28,
                      backgroundColor: theme.colorScheme.primary,
                      child: const Icon(Icons.person_rounded, size: 28, color: Colors.white),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text('Fleet Administrator', style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                          Text('admin@fleetiq.com', style: theme.textTheme.bodySmall),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Settings Groups
            _SettingsGroup(title: 'Application', items: [
              _SettingsTile(icon: Icons.palette_rounded, title: 'Theme', subtitle: 'System default', onTap: () {}),
              _SettingsTile(icon: Icons.language_rounded, title: 'Language', subtitle: 'English (India)', onTap: () {}),
              _SettingsTile(icon: Icons.notifications_rounded, title: 'Notifications', subtitle: 'Enabled', onTap: () {}),
            ]),
            const SizedBox(height: 16),

            _SettingsGroup(title: 'Organization', items: [
              _SettingsTile(icon: Icons.business_rounded, title: 'Tenant', subtitle: 'omega-logistics', onTap: () {}),
              _SettingsTile(icon: Icons.speed_rounded, title: 'API Server', subtitle: 'localhost:8080', onTap: () {}),
            ]),
            const SizedBox(height: 16),

            _SettingsGroup(title: 'Account', items: [
              _SettingsTile(icon: Icons.info_rounded, title: 'About', subtitle: 'FleetIQ v1.0.0', onTap: () {}),
              _SettingsTile(
                icon: Icons.logout_rounded,
                title: 'Sign Out',
                subtitle: 'End current session',
                onTap: () => context.read<AuthBloc>().add(AuthLogoutRequested()),
                isDestructive: true,
              ),
            ]),
          ],
        ),
      ),
    );
  }
}

class _SettingsGroup extends StatelessWidget {
  final String title;
  final List<_SettingsTile> items;

  const _SettingsGroup({required this.title, required this.items});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8),
          child: Text(title, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600)),
        ),
        Card(child: Column(children: items)),
      ],
    );
  }
}

class _SettingsTile extends StatelessWidget {
  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;
  final bool isDestructive;

  const _SettingsTile({required this.icon, required this.title, required this.subtitle, required this.onTap, this.isDestructive = false});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final color = isDestructive ? theme.colorScheme.error : theme.colorScheme.onSurface;

    return ListTile(
      leading: Icon(icon, color: color, size: 22),
      title: Text(title, style: TextStyle(color: color, fontWeight: FontWeight.w500)),
      subtitle: Text(subtitle, style: theme.textTheme.bodySmall),
      trailing: Icon(Icons.chevron_right_rounded, color: theme.colorScheme.onSurfaceVariant),
      onTap: onTap,
    );
  }
}
