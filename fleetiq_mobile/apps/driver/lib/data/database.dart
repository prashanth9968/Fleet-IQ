import 'dart:io';

import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

part 'database.g.dart';

// Tables
class PendingInspections extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get vehicleId => text()();
  TextColumn get checklistJson => text()(); // Store the bool map as JSON string
  DateTimeColumn get timestamp => dateTime()();
  IntColumn get retryCount => integer().withDefault(const Constant(0))();
}

class PendingSosAlerts extends Table {
  IntColumn get id => integer().autoIncrement()();
  TextColumn get driverId => text()();
  TextColumn get latLngJson => text().nullable()();
  DateTimeColumn get timestamp => dateTime()();
  IntColumn get retryCount => integer().withDefault(const Constant(0))();
}

@DriftDatabase(tables: [PendingInspections, PendingSosAlerts])
class AppDatabase extends _$AppDatabase {
  AppDatabase() : super(_openConnection());

  @override
  int get schemaVersion => 1;

  // Queues
  Future<int> queueInspection(String vehicleId, String checklistJson) {
    return into(pendingInspections).insert(PendingInspectionsCompanion.insert(
      vehicleId: vehicleId,
      checklistJson: checklistJson,
      timestamp: DateTime.now(),
    ));
  }

  Future<List<PendingInspection>> getPendingInspections() {
    return select(pendingInspections).get();
  }

  Future<void> removeInspection(int id) {
    return (delete(pendingInspections)..where((t) => t.id.equals(id))).go();
  }

  Future<int> queueSos(String driverId, String? latLngJson) {
    return into(pendingSosAlerts).insert(PendingSosAlertsCompanion.insert(
      driverId: driverId,
      latLngJson: Value(latLngJson),
      timestamp: DateTime.now(),
    ));
  }

  Future<List<PendingSosAlert>> getPendingSosAlerts() {
    return select(pendingSosAlerts).get();
  }

  Future<void> removeSosAlert(int id) {
    return (delete(pendingSosAlerts)..where((t) => t.id.equals(id))).go();
  }
}

LazyDatabase _openConnection() {
  return LazyDatabase(() async {
    final dbFolder = await getApplicationDocumentsDirectory();
    final file = File(p.join(dbFolder.path, 'db.sqlite'));
    return NativeDatabase.createInBackground(file);
  });
}
