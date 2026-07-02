/// Routes notification payloads to app-specific screens.
class NotificationRouter {
  /// Parse a notification payload and return a route path.
  static String? getRoute(String? payload) {
    if (payload == null || payload.isEmpty) return null;

    final parts = payload.split(':');
    if (parts.length < 2) return null;

    final type = parts[0];
    final id = parts[1];

    switch (type) {
      case 'fuel_theft':
        return '/fuel?vehicleId=$id';
      case 'overheat':
        return '/health?vehicleId=$id';
      case 'maintenance':
        return '/maintenance?vehicleId=$id';
      case 'assignment':
        return '/vehicles/$id';
      case 'sos':
        return '/alerts?sos=$id';
      default:
        return '/alerts';
    }
  }
}
