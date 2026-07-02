import 'package:flutter/material.dart';

class TodayTripsScreen extends StatelessWidget {
  const TodayTripsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final trips = [
      {'route': 'Bangalore Depot → Electronic City', 'distance': '18.4 km', 'status': 'COMPLETED', 'time': '06:30 AM - 07:45 AM', 'fuel': '4.2 L'},
      {'route': 'Electronic City → Whitefield Hub', 'distance': '24.6 km', 'status': 'IN_PROGRESS', 'time': '08:15 AM - In transit', 'fuel': '5.8 L'},
      {'route': 'Whitefield Hub → Bangalore Depot', 'distance': '22.0 km', 'status': 'PENDING', 'time': '10:30 AM (Scheduled)', 'fuel': '—'},
    ];

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text("Today's Route", style: theme.textTheme.headlineLarge),
            const SizedBox(height: 4),
            Text('3 trips scheduled • 65.0 km total', style: theme.textTheme.bodyMedium),
            const SizedBox(height: 20),

            // Progress Summary
            Card(
              color: theme.colorScheme.primary,
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceAround,
                  children: [
                    _SummaryItem(value: '1/3', label: 'Completed', color: Colors.white),
                    Container(width: 1, height: 40, color: Colors.white24),
                    _SummaryItem(value: '43 km', label: 'Remaining', color: Colors.white),
                    Container(width: 1, height: 40, color: Colors.white24),
                    _SummaryItem(value: '10 L', label: 'Fuel Used', color: Colors.white),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),

            // Trip List
            ...trips.map((t) {
              final statusColor = t['status'] == 'COMPLETED' ? Colors.green
                  : t['status'] == 'IN_PROGRESS' ? Colors.blue
                  : Colors.grey;
              final icon = t['status'] == 'COMPLETED' ? Icons.check_circle_rounded
                  : t['status'] == 'IN_PROGRESS' ? Icons.play_circle_rounded
                  : Icons.schedule_rounded;

              return Card(
                margin: const EdgeInsets.only(bottom: 12),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Container(
                        width: 44, height: 44,
                        decoration: BoxDecoration(color: statusColor.withOpacity(0.1), borderRadius: BorderRadius.circular(12)),
                        child: Icon(icon, color: statusColor, size: 24),
                      ),
                      const SizedBox(width: 14),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(t['route']!, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.w600)),
                            const SizedBox(height: 6),
                            Row(
                              children: [
                                Icon(Icons.straighten_rounded, size: 13, color: theme.colorScheme.onSurfaceVariant),
                                const SizedBox(width: 4),
                                Text(t['distance']!, style: theme.textTheme.bodySmall),
                                const SizedBox(width: 12),
                                Icon(Icons.schedule_rounded, size: 13, color: theme.colorScheme.onSurfaceVariant),
                                const SizedBox(width: 4),
                                Expanded(child: Text(t['time']!, style: theme.textTheme.bodySmall)),
                              ],
                            ),
                            const SizedBox(height: 6),
                            Row(
                              children: [
                                Icon(Icons.local_gas_station_rounded, size: 13, color: theme.colorScheme.onSurfaceVariant),
                                const SizedBox(width: 4),
                                Text('Fuel: ${t['fuel']}', style: theme.textTheme.bodySmall),
                                const Spacer(),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                  decoration: BoxDecoration(color: statusColor.withOpacity(0.1), borderRadius: BorderRadius.circular(6)),
                                  child: Text(
                                    (t['status']! as String).replaceAll('_', ' '),
                                    style: TextStyle(color: statusColor, fontSize: 10, fontWeight: FontWeight.bold),
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              );
            }),
          ],
        ),
      ),
    );
  }
}

class _SummaryItem extends StatelessWidget {
  final String value;
  final String label;
  final Color color;

  const _SummaryItem({required this.value, required this.label, required this.color});

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(value, style: TextStyle(color: color, fontSize: 20, fontWeight: FontWeight.bold)),
        const SizedBox(height: 4),
        Text(label, style: TextStyle(color: color.withOpacity(0.7), fontSize: 12)),
      ],
    );
  }
}
