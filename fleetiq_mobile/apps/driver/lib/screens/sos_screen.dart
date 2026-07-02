import 'package:flutter/material.dart';

class SosScreen extends StatefulWidget {
  const SosScreen({super.key});

  @override
  State<SosScreen> createState() => _SosScreenState();
}

class _SosScreenState extends State<SosScreen> {
  bool _triggered = false;
  bool _confirming = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Emergency', style: theme.textTheme.headlineLarge),
            const SizedBox(height: 8),
            Text(
              _triggered
                  ? 'SOS signal transmitted. Help is on the way.'
                  : 'Press and hold the button below to send an emergency SOS alert to your fleet operations centre.',
              style: theme.textTheme.bodyMedium,
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 48),

            // SOS Button
            GestureDetector(
              onLongPressStart: (_) {
                if (!_triggered) setState(() => _confirming = true);
              },
              onLongPressEnd: (_) {
                if (_confirming && !_triggered) {
                  setState(() {
                    _confirming = false;
                    _triggered = true;
                  });
                  // TODO: Call API client submitSosAlert or queue in offline database
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('🆘 SOS Alert Sent! Operations centre notified.'), backgroundColor: Colors.red),
                  );
                }
              },
              onLongPressCancel: () {
                setState(() => _confirming = false);
              },
              child: AnimatedContainer(
                duration: const Duration(milliseconds: 300),
                width: _confirming ? 200 : 180,
                height: _confirming ? 200 : 180,
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: _triggered ? Colors.green : Colors.red,
                  boxShadow: [
                    BoxShadow(
                      color: (_triggered ? Colors.green : Colors.red).withOpacity(_confirming ? 0.6 : 0.3),
                      blurRadius: _confirming ? 40 : 20,
                      spreadRadius: _confirming ? 10 : 4,
                    ),
                  ],
                ),
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        _triggered ? Icons.check_rounded : Icons.sos_rounded,
                        size: 56,
                        color: Colors.white,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        _triggered ? 'SENT' : 'SOS',
                        style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold, letterSpacing: 2),
                      ),
                    ],
                  ),
                ),
              ),
            ),
            const SizedBox(height: 32),

            if (!_triggered)
              Text(
                'Hold for 2 seconds to activate',
                style: theme.textTheme.bodySmall?.copyWith(color: Colors.red),
              ),

            if (_triggered) ...[
              const SizedBox(height: 16),
              Card(
                color: Colors.green.withOpacity(0.1),
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Row(
                    children: [
                      const Icon(Icons.info_rounded, color: Colors.green),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Your GPS coordinates and vehicle details have been sent to the operations team. Stay calm and wait for assistance.',
                          style: theme.textTheme.bodySmall,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              OutlinedButton(
                onPressed: () => setState(() => _triggered = false),
                child: const Text('Reset SOS'),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
