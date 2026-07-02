import 'package:flutter/material.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Fleet Dashboard', style: theme.textTheme.headlineLarge),
            const SizedBox(height: 4),
            Text('Real-time operational overview', style: theme.textTheme.bodyMedium),
            const SizedBox(height: 24),

            // KPI Cards Row
            GridView.count(
              crossAxisCount: 2,
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              childAspectRatio: 1.4,
              children: [
                _KpiCard(title: 'Active Vehicles', value: '42', icon: Icons.local_shipping_rounded, color: theme.colorScheme.primary),
                _KpiCard(title: 'Fuel Efficiency', value: '4.2 km/L', icon: Icons.local_gas_station_rounded, color: Colors.amber),
                _KpiCard(title: 'Safety Score', value: '91.2%', icon: Icons.shield_rounded, color: Colors.green),
                _KpiCard(title: 'Active Alerts', value: '3', icon: Icons.warning_rounded, color: Colors.red),
              ],
            ),
            const SizedBox(height: 24),

            // Recent Alerts Section
            Text('Recent Alerts', style: theme.textTheme.titleLarge),
            const SizedBox(height: 12),
            ...[
              _AlertTile(severity: 'CRITICAL', message: 'Engine overheat detected — KA-01-MJ-1024', time: '2 min ago', color: Colors.red),
              _AlertTile(severity: 'HIGH', message: 'Suspected fuel theft — DL-01-AA-5678', time: '15 min ago', color: Colors.orange),
              _AlertTile(severity: 'MEDIUM', message: 'Brake pad wear — KA-03-MK-4512', time: '1 hr ago', color: Colors.amber),
            ],
            const SizedBox(height: 24),

            // Quick Actions
            Text('Quick Actions', style: theme.textTheme.titleLarge),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(child: _ActionCard(icon: Icons.map_rounded, label: 'Live Map', onTap: () {})),
                const SizedBox(width: 12),
                Expanded(child: _ActionCard(icon: Icons.assignment_rounded, label: 'Reports', onTap: () {})),
                const SizedBox(width: 12),
                Expanded(child: _ActionCard(icon: Icons.build_rounded, label: 'Maintenance', onTap: () {})),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _KpiCard extends StatelessWidget {
  final String title;
  final String value;
  final IconData icon;
  final Color color;

  const _KpiCard({required this.title, required this.value, required this.icon, required this.color});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Container(
              padding: const EdgeInsets.all(8),
              decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
              child: Icon(icon, color: color, size: 22),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(value, style: theme.textTheme.headlineMedium?.copyWith(fontWeight: FontWeight.bold)),
                Text(title, style: theme.textTheme.bodySmall),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _AlertTile extends StatelessWidget {
  final String severity;
  final String message;
  final String time;
  final Color color;

  const _AlertTile({required this.severity, required this.message, required this.time, required this.color});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Container(
          width: 40, height: 40,
          decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(10)),
          child: Icon(Icons.warning_rounded, color: color, size: 20),
        ),
        title: Text(message, style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w500)),
        subtitle: Text(time, style: theme.textTheme.bodySmall),
        trailing: Container(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(6)),
          child: Text(severity, style: TextStyle(color: color, fontSize: 10, fontWeight: FontWeight.bold)),
        ),
      ),
    );
  }
}

class _ActionCard extends StatelessWidget {
  final IconData icon;
  final String label;
  final VoidCallback onTap;

  const _ActionCard({required this.icon, required this.label, required this.onTap});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(16),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 20),
          child: Column(
            children: [
              Icon(icon, size: 28, color: theme.colorScheme.primary),
              const SizedBox(height: 8),
              Text(label, style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600)),
            ],
          ),
        ),
      ),
    );
  }
}
