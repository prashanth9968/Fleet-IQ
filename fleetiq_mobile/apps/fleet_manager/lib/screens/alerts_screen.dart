import 'package:flutter/material.dart';

class AlertsScreen extends StatelessWidget {
  const AlertsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final alerts = [
      {'type': 'ENGINE_OVERHEAT', 'severity': 'CRITICAL', 'vehicle': 'KA-01-MJ-1024', 'msg': 'Coolant temperature exceeded 108°C', 'time': '2 min ago'},
      {'type': 'FUEL_THEFT', 'severity': 'HIGH', 'vehicle': 'DL-01-AA-5678', 'msg': 'Stationary fuel drop of 12L detected', 'time': '15 min ago'},
      {'type': 'OVERSPEED', 'severity': 'MEDIUM', 'vehicle': 'KA-03-MK-4512', 'msg': '62 km/h in 30 km/h zone (Warehouse)', 'time': '42 min ago'},
      {'type': 'BRAKE_WEAR', 'severity': 'MEDIUM', 'vehicle': 'MH-04-BX-9012', 'msg': 'Brake pad thickness 2.2mm (limit 3.0mm)', 'time': '1 hr ago'},
      {'type': 'LOW_BATTERY', 'severity': 'HIGH', 'vehicle': 'TN-01-CC-3456', 'msg': 'Battery voltage dropped to 11.2V', 'time': '2 hr ago'},
    ];

    return SafeArea(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 4),
            child: Text('Alert Centre', style: theme.textTheme.headlineLarge),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
            child: Text('${alerts.length} active alerts', style: theme.textTheme.bodyMedium),
          ),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: alerts.length,
              itemBuilder: (context, index) {
                final a = alerts[index];
                final severityColor = a['severity'] == 'CRITICAL' ? Colors.red
                    : a['severity'] == 'HIGH' ? Colors.orange
                    : Colors.amber;
                final icon = a['type'] == 'ENGINE_OVERHEAT' ? Icons.thermostat_rounded
                    : a['type'] == 'FUEL_THEFT' ? Icons.local_gas_station_rounded
                    : a['type'] == 'OVERSPEED' ? Icons.speed_rounded
                    : a['type'] == 'LOW_BATTERY' ? Icons.battery_alert_rounded
                    : Icons.build_rounded;

                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: Padding(
                    padding: const EdgeInsets.all(16),
                    child: Row(
                      children: [
                        Container(
                          width: 48, height: 48,
                          decoration: BoxDecoration(
                            color: severityColor.withOpacity(0.1),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Icon(icon, color: severityColor, size: 24),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                children: [
                                  Expanded(
                                    child: Text(
                                      (a['type'] as String).replaceAll('_', ' '),
                                      style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                                    ),
                                  ),
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                                    decoration: BoxDecoration(color: severityColor.withOpacity(0.1), borderRadius: BorderRadius.circular(6)),
                                    child: Text(a['severity'] as String, style: TextStyle(color: severityColor, fontSize: 10, fontWeight: FontWeight.bold)),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 4),
                              Text(a['msg'] as String, style: theme.textTheme.bodySmall),
                              const SizedBox(height: 6),
                              Row(
                                children: [
                                  Icon(Icons.local_shipping_rounded, size: 12, color: theme.colorScheme.onSurfaceVariant),
                                  const SizedBox(width: 4),
                                  Text(a['vehicle'] as String, style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600)),
                                  const Spacer(),
                                  Text(a['time'] as String, style: theme.textTheme.bodySmall),
                                ],
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }
}
