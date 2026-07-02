// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'fleetiq_models.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

Vehicle _$VehicleFromJson(Map<String, dynamic> json) => Vehicle(
      id: json['id'] as String,
      name: json['name'] as String,
      licensePlate: json['licensePlate'] as String,
      status: json['status'] as String,
      currentLat: (json['currentLat'] as num).toDouble(),
      currentLng: (json['currentLng'] as num).toDouble(),
      fuelLevel: (json['fuelLevel'] as num).toDouble(),
      batteryHealth: (json['batteryHealth'] as num).toDouble(),
    );

Map<String, dynamic> _$VehicleToJson(Vehicle instance) => <String, dynamic>{
      'id': instance.id,
      'name': instance.name,
      'licensePlate': instance.licensePlate,
      'status': instance.status,
      'currentLat': instance.currentLat,
      'currentLng': instance.currentLng,
      'fuelLevel': instance.fuelLevel,
      'batteryHealth': instance.batteryHealth,
    };

Trip _$TripFromJson(Map<String, dynamic> json) => Trip(
      id: json['id'] as String,
      vehicleId: json['vehicleId'] as String,
      driverId: json['driverId'] as String,
      startTime: DateTime.parse(json['startTime'] as String),
      endTime: json['endTime'] == null
          ? null
          : DateTime.parse(json['endTime'] as String),
      startLat: (json['startLat'] as num).toDouble(),
      startLng: (json['startLng'] as num).toDouble(),
      endLat: (json['endLat'] as num?)?.toDouble(),
      endLng: (json['endLng'] as num?)?.toDouble(),
      distanceKm: (json['distanceKm'] as num).toDouble(),
      status: json['status'] as String,
    );

Map<String, dynamic> _$TripToJson(Trip instance) => <String, dynamic>{
      'id': instance.id,
      'vehicleId': instance.vehicleId,
      'driverId': instance.driverId,
      'startTime': instance.startTime.toIso8601String(),
      'endTime': instance.endTime?.toIso8601String(),
      'startLat': instance.startLat,
      'startLng': instance.startLng,
      'endLat': instance.endLat,
      'endLng': instance.endLng,
      'distanceKm': instance.distanceKm,
      'status': instance.status,
    };

DriverScore _$DriverScoreFromJson(Map<String, dynamic> json) => DriverScore(
      driverId: json['driverId'] as String,
      driverName: json['driverName'] as String,
      overallScore: (json['overallScore'] as num).toDouble(),
      harshBrakingCount: (json['harshBrakingCount'] as num).toInt(),
      harshAccelerationCount: (json['harshAccelerationCount'] as num).toInt(),
      speedingCount: (json['speedingCount'] as num).toInt(),
      complianceRate: (json['complianceRate'] as num).toDouble(),
    );

Map<String, dynamic> _$DriverScoreToJson(DriverScore instance) =>
    <String, dynamic>{
      'driverId': instance.driverId,
      'driverName': instance.driverName,
      'overallScore': instance.overallScore,
      'harshBrakingCount': instance.harshBrakingCount,
      'harshAccelerationCount': instance.harshAccelerationCount,
      'speedingCount': instance.speedingCount,
      'complianceRate': instance.complianceRate,
    };

AlertHistory _$AlertHistoryFromJson(Map<String, dynamic> json) => AlertHistory(
      id: json['id'] as String,
      vehicleId: json['vehicleId'] as String,
      severity: json['severity'] as String,
      message: json['message'] as String,
      timestamp: DateTime.parse(json['timestamp'] as String),
      isRead: json['isRead'] as bool,
    );

Map<String, dynamic> _$AlertHistoryToJson(AlertHistory instance) =>
    <String, dynamic>{
      'id': instance.id,
      'vehicleId': instance.vehicleId,
      'severity': instance.severity,
      'message': instance.message,
      'timestamp': instance.timestamp.toIso8601String(),
      'isRead': instance.isRead,
    };
