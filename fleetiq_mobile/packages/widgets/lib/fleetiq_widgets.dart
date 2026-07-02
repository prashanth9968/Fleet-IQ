import 'package:flutter/material.dart';
import 'package:fleetiq_models/fleetiq_models.dart';
import 'package:fleetiq_theme/fleetiq_theme.dart';

/// A gauge that displays a percentage or score with a circular progress indicator.
class MetricGauge extends StatelessWidget {
  final String title;
  final double value; // Assumed to be 0.0 to 100.0 or 0.0 to 1.0
  final double max;
  final String unit;
  final Color? color;

  const MetricGauge({
    Key? key,
    required this.title,
    required this.value,
    this.max = 100.0,
    this.unit = '%',
    this.color,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final percentage = (value / max).clamp(0.0, 1.0);
    final theme = Theme.of(context);
    final indicatorColor = color ??
        (percentage < 0.3
            ? theme.colorScheme.error
            : percentage < 0.7
                ? Colors.amber
                : theme.colorScheme.primary);

    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              title,
              style: theme.textTheme.titleMedium?.copyWith(
                fontWeight: FontWeight.bold,
                color: theme.colorScheme.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 16),
            Stack(
              alignment: Alignment.center,
              children: [
                SizedBox(
                  width: 80,
                  height: 80,
                  child: CircularProgressIndicator(
                    value: percentage,
                    strokeWidth: 8,
                    backgroundColor: theme.colorScheme.surfaceVariant,
                    valueColor: AlwaysStoppedAnimation<Color>(indicatorColor),
                  ),
                ),
                Text(
                  '${value.toStringAsFixed(0)}$unit',
                  style: theme.textTheme.titleLarge?.copyWith(
                    fontWeight: FontWeight.bold,
                    color: theme.colorScheme.onSurface,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// A badge that displays a status with appropriate color coding.
class StatusBadge extends StatelessWidget {
  final String status;

  const StatusBadge({
    Key? key,
    required this.status,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    Color backgroundColor;
    Color textColor;

    switch (status.toLowerCase()) {
      case 'active':
      case 'online':
      case 'ongoing':
      case 'completed':
        backgroundColor = Colors.green.shade50;
        textColor = Colors.green.shade700;
        break;
      case 'inactive':
      case 'offline':
      case 'cancelled':
        backgroundColor = theme.colorScheme.errorContainer;
        textColor = theme.colorScheme.onErrorContainer;
        break;
      case 'maintenance':
      case 'warning':
        backgroundColor = Colors.amber.shade50;
        textColor = Colors.amber.shade800;
        break;
      default:
        backgroundColor = theme.colorScheme.surfaceVariant;
        textColor = theme.colorScheme.onSurfaceVariant;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: backgroundColor,
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        status.toUpperCase(),
        style: theme.textTheme.labelMedium?.copyWith(
          color: textColor,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }
}

/// A list row/card representation of an alert.
class AlertRowCard extends StatelessWidget {
  final AlertHistory alert;
  final VoidCallback? onTap;

  const AlertRowCard({
    Key? key,
    required this.alert,
    this.onTap,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    IconData icon;
    Color iconColor;
    Color cardBorderColor;

    switch (alert.severity.toUpperCase()) {
      case 'CRITICAL':
        icon = Icons.error_outline;
        iconColor = theme.colorScheme.error;
        cardBorderColor = theme.colorScheme.error.withOpacity(0.5);
        break;
      case 'WARNING':
        icon = Icons.warning_amber_outlined;
        iconColor = Colors.amber.shade700;
        cardBorderColor = Colors.amber.shade500.withOpacity(0.5);
        break;
      default:
        icon = Icons.info_outline;
        iconColor = theme.colorScheme.primary;
        cardBorderColor = theme.colorScheme.primary.withOpacity(0.5);
    }

    return Card(
      elevation: alert.isRead ? 1 : 3,
      shape: RoundedRectangleBorder(
        side: alert.isRead
            ? BorderSide.none
            : BorderSide(color: cardBorderColor, width: 1.5),
        borderRadius: BorderRadius.circular(12),
      ),
      margin: const EdgeInsets.symmetric(vertical: 6, horizontal: 8),
      child: ListTile(
        onTap: onTap,
        leading: CircleAvatar(
          backgroundColor: iconColor.withOpacity(0.1),
          child: Icon(icon, color: iconColor),
        ),
        title: Text(
          alert.message,
          style: theme.textTheme.bodyMedium?.copyWith(
            fontWeight: alert.isRead ? FontWeight.normal : FontWeight.bold,
          ),
        ),
        subtitle: Text(
          'Vehicle: ${alert.vehicleId} • ${_formatTime(alert.timestamp)}',
          style: theme.textTheme.bodySmall,
        ),
        trailing: alert.isRead
            ? null
            : Container(
                width: 8,
                height: 8,
                decoration: BoxDecoration(
                  color: iconColor,
                  shape: BoxShape.circle,
                ),
              ),
      ),
    );
  }

  String _formatTime(DateTime dateTime) {
    return '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}';
  }
}

/// A customized elevated or outlined button matching FleetIQ theme specs.
class CustomButton extends StatelessWidget {
  final String label;
  final VoidCallback? onPressed;
  final bool isPrimary;
  final bool isLoading;
  final IconData? icon;

  const CustomButton({
    Key? key,
    required this.label,
    this.onPressed,
    this.isPrimary = true,
    this.isLoading = false,
    this.icon,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final buttonShape = RoundedRectangleBorder(
      borderRadius: BorderRadius.circular(12),
    );

    final Widget content = Row(
      mainAxisSize: MainAxisSize.min,
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (isLoading) ...[
          SizedBox(
            width: 18,
            height: 18,
            child: CircularProgressIndicator(
              strokeWidth: 2,
              valueColor: AlwaysStoppedAnimation<Color>(
                isPrimary ? theme.colorScheme.onPrimary : theme.colorScheme.primary,
              ),
            ),
          ),
          const SizedBox(width: 8),
        ] else if (icon != null) ...[
          Icon(icon, size: 18),
          const SizedBox(width: 8),
        ],
        Text(
          label,
          style: const TextStyle(fontWeight: FontWeight.w600),
        ),
      ],
    );

    if (isPrimary) {
      return ElevatedButton(
        onPressed: isLoading ? null : onPressed,
        style: ElevatedButton.styleFrom(
          shape: buttonShape,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
          elevation: 2,
        ),
        child: content,
      );
    } else {
      return OutlinedButton(
        onPressed: isLoading ? null : onPressed,
        style: OutlinedButton.styleFrom(
          shape: buttonShape,
          padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        ),
        child: content,
      );
    }
  }
}
