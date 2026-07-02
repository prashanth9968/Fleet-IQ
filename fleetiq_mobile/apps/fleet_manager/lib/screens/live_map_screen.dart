import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

class LiveMapScreen extends StatelessWidget {
  const LiveMapScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    // Demo vehicle positions (Bangalore area)
    final vehicles = [
      {'reg': 'KA-01-MJ-1024', 'lat': 12.9716, 'lng': 77.5946, 'status': 'MOVING'},
      {'reg': 'KA-03-MK-4512', 'lat': 12.9352, 'lng': 77.6245, 'status': 'IDLE'},
      {'reg': 'DL-01-AA-5678', 'lat': 12.9850, 'lng': 77.5533, 'status': 'MOVING'},
    ];

    return Scaffold(
      body: SafeArea(
        child: Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 16, 20, 12),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text('Live Tracking', style: theme.textTheme.headlineLarge),
                      Text('${vehicles.length} vehicles online', style: theme.textTheme.bodyMedium),
                    ],
                  ),
                  FilledButton.icon(
                    onPressed: () {},
                    icon: const Icon(Icons.refresh_rounded, size: 18),
                    label: const Text('Refresh'),
                  ),
                ],
              ),
            ),
            Expanded(
              child: ClipRRect(
                borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
                child: FlutterMap(
                  options: const MapOptions(
                    initialCenter: LatLng(12.9716, 77.5946),
                    initialZoom: 12.5,
                  ),
                  children: [
                    TileLayer(
                      urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                      userAgentPackageName: 'com.fleetiq.manager',
                    ),
                    MarkerLayer(
                      markers: vehicles.map((v) {
                        final color = v['status'] == 'MOVING' ? Colors.green : Colors.amber;
                        return Marker(
                          point: LatLng(v['lat'] as double, v['lng'] as double),
                          width: 44,
                          height: 44,
                          child: Container(
                            decoration: BoxDecoration(
                              color: color,
                              shape: BoxShape.circle,
                              boxShadow: [BoxShadow(color: color.withOpacity(0.4), blurRadius: 8, spreadRadius: 2)],
                            ),
                            child: const Icon(Icons.local_shipping_rounded, color: Colors.white, size: 22),
                          ),
                        );
                      }).toList(),
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
