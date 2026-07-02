import 'dart:async';
import 'package:dio/dio.dart';
import '../data/database.dart';
import 'package:fleetiq_shared/fleetiq_shared.dart';

class SyncService {
  final AppDatabase db;
  final Dio dio;
  Timer? _timer;

  SyncService({required this.db, required this.dio});

  void startSyncTimer() {
    // Run sync every 30 seconds
    _timer = Timer.periodic(const Duration(seconds: 30), (_) => syncNow());
  }

  void stopSyncTimer() {
    _timer?.cancel();
  }

  Future<void> syncNow() async {
    FleetLogger.info('Starting background sync cycle...');
    await _syncInspections();
    await _syncSosAlerts();
  }

  Future<void> _syncInspections() async {
    final pending = await db.getPendingInspections();
    for (final item in pending) {
      try {
        await dio.post('/inspections', data: {
          'vehicleId': item.vehicleId,
          'checklist': item.checklistJson,
          'timestamp': item.timestamp.toIso8601String(),
        });
        await db.removeInspection(item.id);
        FleetLogger.info('Synced inspection ${item.id}');
      } catch (e) {
        FleetLogger.error('Failed to sync inspection ${item.id}: $e');
        // Will retry on next cycle
      }
    }
  }

  Future<void> _syncSosAlerts() async {
    final pending = await db.getPendingSosAlerts();
    for (final item in pending) {
      try {
        await dio.post('/alerts/sos', data: {
          'driverId': item.driverId,
          'location': item.latLngJson,
          'timestamp': item.timestamp.toIso8601String(),
        });
        await db.removeSosAlert(item.id);
        FleetLogger.info('Synced SOS alert ${item.id}');
      } catch (e) {
        FleetLogger.error('Failed to sync SOS alert ${item.id}: $e');
      }
    }
  }
}
