import 'package:json_annotation/json_annotation.dart';

part 'fleetiq_models.g.dart';

@JsonSerializable()
class Vehicle {
  final String id;
  final String name;
  final String licensePlate;
  final String status;
  final double currentLat;
  final double currentLng;
  final double fuelLevel;
  final double batteryHealth;

  const Vehicle({
    required this.id,
    required this.name,
    required this.licensePlate,
    required this.status,
    required this.currentLat,
    required this.currentLng,
    required this.fuelLevel,
    required this.batteryHealth,
  });

  factory Vehicle.fromJson(Map<String, dynamic> json) => _$VehicleFromJson(json);
  Map<String, dynamic> toJson() => _$VehicleToJson(this);

  // Helper copyWith method
  Vehicle copyWith({
    String? id,
    String? name,
    String? licensePlate,
    String? status,
    double? currentLat,
    double? currentLng,
    double? fuelLevel,
    double? batteryHealth,
  }) {
    return Vehicle(
      id: id ?? this.id,
      name: name ?? this.name,
      licensePlate: licensePlate ?? this.licensePlate,
      status: status ?? this.status,
      currentLat: currentLat ?? this.currentLat,
      currentLng: currentLng ?? this.currentLng,
      fuelLevel: fuelLevel ?? this.fuelLevel,
      batteryHealth: batteryHealth ?? this.batteryHealth,
    );
  }
}

@JsonSerializable()
class Trip {
  final String id;
  final String vehicleId;
  final String driverId;
  final DateTime startTime;
  final DateTime? endTime;
  final double startLat;
  final double startLng;
  final double? endLat;
  final double? endLng;
  final double distanceKm;
  final String status;

  const Trip({
    required this.id,
    required this.vehicleId,
    required this.driverId,
    required this.startTime,
    this.endTime,
    required this.startLat,
    required this.startLng,
    this.endLat,
    this.endLng,
    required this.distanceKm,
    required this.status,
  });

  factory Trip.fromJson(Map<String, dynamic> json) => _$TripFromJson(json);
  Map<String, dynamic> toJson() => _$TripToJson(this);

  Trip copyWith({
    String? id,
    String? vehicleId,
    String? driverId,
    DateTime? startTime,
    DateTime? endTime,
    double? startLat,
    double? startLng,
    double? endLat,
    double? endLng,
    double? distanceKm,
    String? status,
  }) {
    return Trip(
      id: id ?? this.id,
      vehicleId: vehicleId ?? this.vehicleId,
      driverId: driverId ?? this.driverId,
      startTime: startTime ?? this.startTime,
      endTime: endTime ?? this.endTime,
      startLat: startLat ?? this.startLat,
      startLng: startLng ?? this.startLng,
      endLat: endLat ?? this.endLat,
      endLng: endLng ?? this.endLng,
      distanceKm: distanceKm ?? this.distanceKm,
      status: status ?? this.status,
    );
  }
}

@JsonSerializable()
class DriverScore {
  final String driverId;
  final String driverName;
  final double overallScore;
  final int harshBrakingCount;
  final int harshAccelerationCount;
  final int speedingCount;
  final double complianceRate;

  const DriverScore({
    required this.driverId,
    required this.driverName,
    required this.overallScore,
    required this.harshBrakingCount,
    required this.harshAccelerationCount,
    required this.speedingCount,
    required this.complianceRate,
  });

  factory DriverScore.fromJson(Map<String, dynamic> json) => _$DriverScoreFromJson(json);
  Map<String, dynamic> toJson() => _$DriverScoreToJson(this);

  DriverScore copyWith({
    String? driverId,
    String? driverName,
    double? overallScore,
    int? harshBrakingCount,
    int? harshAccelerationCount,
    int? speedingCount,
    double? complianceRate,
  }) {
    return DriverScore(
      driverId: driverId ?? this.driverId,
      driverName: driverName ?? this.driverName,
      overallScore: overallScore ?? this.overallScore,
      harshBrakingCount: harshBrakingCount ?? this.harshBrakingCount,
      harshAccelerationCount: harshAccelerationCount ?? this.harshAccelerationCount,
      speedingCount: speedingCount ?? this.speedingCount,
      complianceRate: complianceRate ?? this.complianceRate,
    );
  }
}

@JsonSerializable()
class AlertHistory {
  final String id;
  final String vehicleId;
  final String severity; // e.g. CRITICAL, WARNING, INFO
  final String message;
  final DateTime timestamp;
  final bool isRead;

  const AlertHistory({
    required this.id,
    required this.vehicleId,
    required this.severity,
    required this.message,
    required this.timestamp,
    required this.isRead,
  });

  factory AlertHistory.fromJson(Map<String, dynamic> json) => _$AlertHistoryFromJson(json);
  Map<String, dynamic> toJson() => _$AlertHistoryToJson(this);

  AlertHistory copyWith({
    String? id,
    String? vehicleId,
    String? severity,
    String? message,
    DateTime? timestamp,
    bool? isRead,
  }) {
    return AlertHistory(
      id: id ?? this.id,
      vehicleId: vehicleId ?? this.vehicleId,
      severity: severity ?? this.severity,
      message: message ?? this.message,
      timestamp: timestamp ?? this.timestamp,
      isRead: isRead ?? this.isRead,
    );
  }
}
