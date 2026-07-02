import 'package:flutter/material.dart';

class SafetyScoreScreen extends StatelessWidget {
  const SafetyScoreScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    const score = 87.5;

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Safety Score', style: theme.textTheme.headlineLarge),
            const SizedBox(height: 4),
            Text('Drive safely. Score resets monthly.', style: theme.textTheme.bodyMedium),
            const SizedBox(height: 24),

            // Score Ring
            Center(
              child: Container(
                width: 180, height: 180,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  gradient: LinearGradient(
                    begin: Alignment.topLeft, end: Alignment.bottomRight,
                    colors: [theme.colorScheme.primary, theme.colorScheme.primary.withOpacity(0.6)],
                  ),
                  boxShadow: [BoxShadow(color: theme.colorScheme.primary.withOpacity(0.3), blurRadius: 20, spreadRadius: 4)],
                ),
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('${score.toStringAsFixed(1)}', style: const TextStyle(color: Colors.white, fontSize: 42, fontWeight: FontWeight.bold)),
                      const Text('/ 100', style: TextStyle(color: Colors.white70, fontSize: 14)),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 32),

            // Breakdown
            Text('Score Breakdown', style: theme.textTheme.titleLarge),
            const SizedBox(height: 12),
            _ScoreRow(label: 'Harsh Braking', count: 3, deduction: -6, icon: Icons.warning_rounded, color: Colors.orange),
            _ScoreRow(label: 'Speeding Violations', count: 1, deduction: -5, icon: Icons.speed_rounded, color: Colors.red),
            _ScoreRow(label: 'Phone Usage', count: 0, deduction: 0, icon: Icons.phone_android_rounded, color: Colors.green),
            _ScoreRow(label: 'Fatigue Detection', count: 0, deduction: 0, icon: Icons.visibility_off_rounded, color: Colors.green),
            _ScoreRow(label: 'Seatbelt Violation', count: 1, deduction: -1.5, icon: Icons.airline_seat_recline_normal_rounded, color: Colors.amber),
            const SizedBox(height: 24),

            // Tips
            Card(
              color: theme.colorScheme.primary.withOpacity(0.05),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    Icon(Icons.lightbulb_rounded, color: theme.colorScheme.primary),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        'Avoid sudden braking. Maintain a 3-second following distance to improve your score.',
                        style: theme.textTheme.bodySmall,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ScoreRow extends StatelessWidget {
  final String label;
  final int count;
  final double deduction;
  final IconData icon;
  final Color color;

  const _ScoreRow({required this.label, required this.count, required this.deduction, required this.icon, required this.color});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.only(bottom: 8),
      child: ListTile(
        leading: Container(
          width: 40, height: 40,
          decoration: BoxDecoration(color: color.withOpacity(0.1), borderRadius: BorderRadius.circular(10)),
          child: Icon(icon, color: color, size: 20),
        ),
        title: Text(label, style: theme.textTheme.bodyMedium?.copyWith(fontWeight: FontWeight.w500)),
        subtitle: Text('$count incidents this month', style: theme.textTheme.bodySmall),
        trailing: Text(
          deduction == 0 ? 'Clean' : '${deduction.toStringAsFixed(1)} pts',
          style: TextStyle(
            color: deduction == 0 ? Colors.green : Colors.red,
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
        ),
      ),
    );
  }
}
