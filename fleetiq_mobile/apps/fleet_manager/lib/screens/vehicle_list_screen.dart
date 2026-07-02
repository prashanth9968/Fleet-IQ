import 'package:flutter/material.dart';

class VehicleListScreen extends StatelessWidget {
  const VehicleListScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    final vehicles = [
      {'reg': 'KA-01-MJ-1024', 'model': 'Ashok Leyland Dost', 'status': 'MOVING', 'fuel': 72.0, 'health': 95.0, 'driver': 'John Doe'},
      {'reg': 'KA-03-MK-4512', 'model': 'Tata Ace Gold', 'status': 'IDLE', 'fuel': 45.0, 'health': 88.0, 'driver': 'Ramesh K'},
      {'reg': 'DL-01-AA-5678', 'model': 'Mahindra Bolero Pickup', 'status': 'MOVING', 'fuel': 60.0, 'health': 92.0, 'driver': 'Robert V'},
      {'reg': 'MH-04-BX-9012', 'model': 'Eicher Pro 2049', 'status': 'MAINTENANCE', 'fuel': 30.0, 'health': 72.0, 'driver': 'Unassigned'},
      {'reg': 'TN-01-CC-3456', 'model': 'BharatBenz 1215R', 'status': 'MOVING', 'fuel': 82.0, 'health': 97.0, 'driver': 'Suresh P'},
    ];

    return SafeArea(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 4),
            child: Text('Fleet Vehicles', style: theme.textTheme.headlineLarge),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 16),
            child: Text('${vehicles.length} vehicles registered', style: theme.textTheme.bodyMedium),
          ),
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              itemCount: vehicles.length,
              itemBuilder: (context, index) {
                final v = vehicles[index];
                final statusColor = v['status'] == 'MOVING' ? Colors.green
                    : v['status'] == 'IDLE' ? Colors.amber
                    : Colors.red;

                return Card(
                  margin: const EdgeInsets.only(bottom: 12),
                  child: InkWell(
                    borderRadius: BorderRadius.circular(16),
                    onTap: () {},
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        children: [
                          Row(
                            children: [
                              Container(
                                width: 48, height: 48,
                                decoration: BoxDecoration(
                                  color: theme.colorScheme.primary.withOpacity(0.1),
                                  borderRadius: BorderRadius.circular(12),
                                ),
                                child: Icon(Icons.local_shipping_rounded, color: theme.colorScheme.primary),
                              ),
                              const SizedBox(width: 14),
                              Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(v['reg'] as String, style: theme.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold)),
                                    Text(v['model'] as String, style: theme.textTheme.bodySmall),
                                  ],
                                ),
                              ),
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                                decoration: BoxDecoration(color: statusColor.withOpacity(0.1), borderRadius: BorderRadius.circular(8)),
                                child: Text(v['status'] as String, style: TextStyle(color: statusColor, fontSize: 11, fontWeight: FontWeight.bold)),
                              ),
                            ],
                          ),
                          const SizedBox(height: 14),
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              _MiniStat(icon: Icons.local_gas_station_rounded, label: 'Fuel', value: '${(v['fuel'] as double).toInt()}%'),
                              _MiniStat(icon: Icons.favorite_rounded, label: 'Health', value: '${(v['health'] as double).toInt()}%'),
                              _MiniStat(icon: Icons.person_rounded, label: 'Driver', value: v['driver'] as String),
                            ],
                          ),
                        ],
                      ),
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

class _MiniStat extends StatelessWidget {
  final IconData icon;
  final String label;
  final String value;

  const _MiniStat({required this.icon, required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Icon(icon, size: 14, color: theme.colorScheme.onSurfaceVariant),
        const SizedBox(width: 4),
        Text('$label: ', style: theme.textTheme.bodySmall),
        Text(value, style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600)),
      ],
    );
  }
}
