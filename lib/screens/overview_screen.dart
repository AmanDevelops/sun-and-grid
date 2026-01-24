import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../providers/data_provider.dart';

class OverviewScreen extends StatefulWidget {
  const OverviewScreen({super.key});

  @override
  State<OverviewScreen> createState() => _OverviewScreenState();
}

class _OverviewScreenState extends State<OverviewScreen> with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<DataProvider>(context);
    
    double solarPower = 0.0;
    double gridPower = 0.0;
    
    double dailyYield = 0.0;
    double totalYield = 0.0;
    
    String statusText = "Normal Mode";
    Color statusColor = const Color(0xFF4FC3F7);

    if (provider.data != null && provider.data!['Data'] is List) {
       final d = provider.data!['Data'] as List;
       if (d.length > 21) {
         solarPower = (d[13] as num).toDouble(); 
         gridPower = (d[11] as num).toDouble();
         dailyYield = (d[21] as num).toDouble() * 0.1;
         totalYield = (d[19] as num).toDouble() * 0.1;
       }
    }

    return Scaffold(
      backgroundColor: Colors.transparent,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        automaticallyImplyLeading: false,
        title: Column(
          children: [
            Text('Local Information', style: GoogleFonts.poppins(color: Colors.black87, fontWeight: FontWeight.bold, fontSize: 20)),
            const SizedBox(height: 4),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
              decoration: BoxDecoration(
                color: statusColor.withOpacity(0.15),
                borderRadius: BorderRadius.circular(20),
              ),
              child: Text(statusText, style: GoogleFonts.poppins(color: statusColor, fontSize: 12, fontWeight: FontWeight.w600)),
            )
          ],
        ),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.settings_outlined, color: Colors.black87),
            onPressed: () {
               _showSettingsDialog(context);
            },
          )
        ],
      ),
      body: LayoutBuilder(
        builder: (context, constraints) {
          return SingleChildScrollView(
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: constraints.maxHeight),
              child: Column(
                children: [
                   const SizedBox(height: 30),
                   // Flow Chart Area
                   SizedBox(
                     height: 380, // Increased height for better spacing
                     child: AnimatedBuilder(
                       animation: _controller,
                       builder: (context, child) {
                         return Stack(
                           alignment: Alignment.center,
                           children: [
                             // Lines & Animation
                             CustomPaint(
                               size: const Size(double.infinity, 380),
                               painter: FlowPainter(
                                 solarPower: solarPower,
                                 animationValue: _controller.value,
                               ),
                             ),
                             
                             // Solar (Left)
                             Positioned(
                               left: 30,
                               child: _buildNode('Solar', solarPower, 'W', Icons.wb_sunny_rounded, Colors.orange),
                             ),
                             
                             // Grid (Right)
                             Positioned(
                               right: 30,
                               child: _buildNode('Grid', gridPower, 'W', Icons.grid_on_rounded, Colors.grey),
                             ),
                           ],
                         );
                       },
                     ),
                   ),
                   
                   const SizedBox(height: 20),
                   
                   // Yield Cards
                   Padding(
                     padding: const EdgeInsets.symmetric(horizontal: 24.0),
                     child: Container(
                       padding: const EdgeInsets.all(24),
                       decoration: BoxDecoration(
                         color: Colors.white,
                         borderRadius: BorderRadius.circular(24),
                         boxShadow: [
                           BoxShadow(
                             color: Colors.black.withOpacity(0.04),
                             blurRadius: 20,
                             offset: const Offset(0, 8),
                           )
                         ]
                       ),
                       child: Row(
                         children: [
                           Expanded(
                             child: Column(
                               crossAxisAlignment: CrossAxisAlignment.start,
                               children: [
                                 Text('Daily Yield', style: GoogleFonts.poppins(color: Colors.grey[500], fontSize: 13, fontWeight: FontWeight.w500)),
                                 const SizedBox(height: 6),
                                 Text('${dailyYield.toStringAsFixed(1)} kWh', style: GoogleFonts.poppins(fontWeight: FontWeight.bold, fontSize: 20, color: Colors.black87)),
                               ],
                             ),
                           ),
                           Container(width: 1, height: 40, color: Colors.grey.withOpacity(0.15)),
                           const SizedBox(width: 24),
                           Expanded(
                             child: Column(
                               crossAxisAlignment: CrossAxisAlignment.start,
                               children: [
                                 Text('Total Yield', style: GoogleFonts.poppins(color: Colors.grey[500], fontSize: 13, fontWeight: FontWeight.w500)),
                                 const SizedBox(height: 6),
                                 Text('${totalYield.toStringAsFixed(1)} kWh', style: GoogleFonts.poppins(fontWeight: FontWeight.bold, fontSize: 20, color: Colors.black87)),
                               ],
                             ),
                           ),
                         ],
                       ),
                     ),
                   ),
                   const SizedBox(height: 20),
                ],
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildNode(String label, double? value, String unit, IconData icon, Color color) {
    double size = 110; // Uniform big size
    bool isActive = value != null && value > 0;
    Color displayColor = isActive ? color : Colors.grey[300]!;
    Color textColor = isActive ? Colors.black87 : Colors.grey;
    Color ringColor = isActive ? color : Colors.grey[200]!;

    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: size,
          height: size,
          decoration: BoxDecoration(
            color: Colors.white,
            shape: BoxShape.circle,
            border: Border.all(color: ringColor.withOpacity(isActive ? 1.0 : 0.5), width: 3),
            boxShadow: [
              BoxShadow(
                color: ringColor.withOpacity(0.15),
                blurRadius: 15,
                offset: const Offset(0, 6),
              )
            ]
          ),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(icon, color: displayColor, size: 32),
              if (value != null) ...[
                const SizedBox(height: 4),
                Text(value.toStringAsFixed(1), style: GoogleFonts.poppins(fontWeight: FontWeight.bold, fontSize: 20, color: textColor)),
                Text(unit, style: GoogleFonts.poppins(fontSize: 12, color: Colors.grey[500], fontWeight: FontWeight.w500)),
              ]
            ],
          ),
        ),
        const SizedBox(height: 12),
        Text(label, style: GoogleFonts.poppins(color: Colors.grey[600], fontSize: 14, fontWeight: FontWeight.w500)),
      ],
    );
  }

  void _showSettingsDialog(BuildContext context) {
    final provider = Provider.of<DataProvider>(context, listen: false);
    final ipController = TextEditingController(text: provider.ipAddress);
    final pwdController = TextEditingController(text: provider.password);

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Settings', style: GoogleFonts.poppins(fontWeight: FontWeight.bold)),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: ipController,
              decoration: const InputDecoration(labelText: 'IP Address'),
            ),
            const SizedBox(height: 10),
            TextField(
              controller: pwdController,
              decoration: const InputDecoration(labelText: 'Password/SN'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
          ElevatedButton(
            onPressed: () {
              provider.setSettings(ipController.text, pwdController.text);
              Navigator.pop(ctx);
              provider.fetchData();
            },
            child: const Text('Save'),
          ),
        ],
      ),
    );
  }
}

class FlowPainter extends CustomPainter {
  final double solarPower;
  final double animationValue;
  
  FlowPainter({required this.solarPower, required this.animationValue});

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.grey.withOpacity(0.15)
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final activePaint = Paint()
      ..color = Colors.orange
      ..strokeWidth = 3
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final centerY = size.height / 2;
    
    // Left Node Center (Solar)
    // Left: 30, Size: 110 => CenterX: 30 + 55 = 85
    final solarCenter = Offset(85, centerY);
    
    // Right Node Center (Grid)
    // Right: 30, Size: 110 => CenterX: Width - 30 - 55 = Width - 85
    final gridCenter = Offset(size.width - 85, centerY);
    
    // Draw connection line
    canvas.drawLine(solarCenter, gridCenter, paint);
    
    // Active State
    if (solarPower > 0) {
      canvas.drawLine(solarCenter, gridCenter, activePaint);
      
      // Pulse Animation
      // Calculate current position based on animation value (0.0 -> 1.0)
      final currentPos = Offset.lerp(solarCenter, gridCenter, animationValue)!;
      
      // Draw Glow
      final glowPaint = Paint()
        ..color = Colors.orange.withOpacity(0.3)
        ..style = PaintingStyle.fill;
      canvas.drawCircle(currentPos, 12, glowPaint);
      
      // Draw Core Particle
      final corePaint = Paint()
        ..color = Colors.white
        ..style = PaintingStyle.fill;
      canvas.drawCircle(currentPos, 4, corePaint);
      
      // Draw Border for particle
      canvas.drawCircle(currentPos, 4, Paint()..color = Colors.orange ..style = PaintingStyle.stroke ..strokeWidth = 2);
    }
  }

  @override
  bool shouldRepaint(covariant FlowPainter oldDelegate) {
    return oldDelegate.solarPower != solarPower || oldDelegate.animationValue != animationValue;
  }
}


