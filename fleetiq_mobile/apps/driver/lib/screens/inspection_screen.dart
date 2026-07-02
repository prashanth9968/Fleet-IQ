import 'package:flutter/material.dart';

class InspectionScreen extends StatefulWidget {
  const InspectionScreen({super.key});

  @override
  State<InspectionScreen> createState() => _InspectionScreenState();
}

class _InspectionScreenState extends State<InspectionScreen> {
  final Map<String, bool> _checklist = {
    'Engine oil level': false,
    'Coolant level': false,
    'Brake fluid': false,
    'Tire pressure (all 4)': false,
    'Headlights & tail lights': false,
    'Indicators & hazard lights': false,
    'Windshield wipers': false,
    'Horn': false,
    'Mirrors': false,
    'Seatbelt': false,
    'Fire extinguisher': false,
    'First aid kit': false,
    'Fuel level sufficient': false,
    'Dashboard warning lights clear': false,
  };

  bool _submitted = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final completedCount = _checklist.values.where((v) => v).length;
    final totalCount = _checklist.length;
    final allChecked = completedCount == totalCount;

    return SafeArea(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 20, 20, 4),
            child: Text('Pre-Trip Inspection', style: theme.textTheme.headlineLarge),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 0, 20, 8),
            child: Text('Complete all checks before starting your shift', style: theme.textTheme.bodyMedium),
          ),

          // Progress Bar
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text('$completedCount / $totalCount items checked', style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.w600)),
                    Text('${(completedCount / totalCount * 100).toInt()}%', style: theme.textTheme.bodySmall?.copyWith(fontWeight: FontWeight.bold, color: theme.colorScheme.primary)),
                  ],
                ),
                const SizedBox(height: 6),
                ClipRRect(
                  borderRadius: BorderRadius.circular(6),
                  child: LinearProgressIndicator(
                    value: completedCount / totalCount,
                    minHeight: 8,
                    backgroundColor: theme.colorScheme.surfaceVariant,
                    valueColor: AlwaysStoppedAnimation(allChecked ? Colors.green : theme.colorScheme.primary),
                  ),
                ),
              ],
            ),
          ),

          // Checklist
          Expanded(
            child: ListView(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              children: _checklist.entries.map((entry) {
                return Card(
                  margin: const EdgeInsets.only(bottom: 6),
                  child: CheckboxListTile(
                    value: entry.value,
                    onChanged: _submitted ? null : (val) {
                      setState(() => _checklist[entry.key] = val ?? false);
                    },
                    title: Text(entry.key, style: theme.textTheme.bodyMedium?.copyWith(
                      decoration: entry.value ? TextDecoration.lineThrough : null,
                      color: entry.value ? theme.colorScheme.onSurfaceVariant : null,
                    )),
                    secondary: Icon(
                      entry.value ? Icons.check_circle_rounded : Icons.radio_button_unchecked_rounded,
                      color: entry.value ? Colors.green : theme.colorScheme.onSurfaceVariant,
                    ),
                    controlAffinity: ListTileControlAffinity.trailing,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                  ),
                );
              }).toList(),
            ),
          ),

          // Submit Button
          Padding(
            padding: const EdgeInsets.all(20),
            child: SizedBox(
              width: double.infinity,
              height: 56,
              child: ElevatedButton.icon(
                onPressed: _submitted ? null : () {
                  if (!allChecked) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('Please complete all ${totalCount - completedCount} remaining items'), backgroundColor: Colors.amber),
                    );
                    return;
                  }
                  setState(() => _submitted = true);
                  // TODO: Sync to API or queue in local SQLite for offline submission
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('✅ Inspection submitted. Saved locally for sync.'), backgroundColor: Colors.green),
                  );
                },
                icon: Icon(_submitted ? Icons.check_circle : Icons.upload_rounded),
                label: Text(_submitted ? 'Submitted' : 'Submit Inspection', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                style: ElevatedButton.styleFrom(
                  backgroundColor: _submitted ? Colors.green : null,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
