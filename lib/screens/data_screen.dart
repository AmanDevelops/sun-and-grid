import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../providers/data_provider.dart';

class DataScreen extends StatelessWidget {
  const DataScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final provider = Provider.of<DataProvider>(context);
    final data = provider.data;

    if (data == null || !data.containsKey('Data') || data['Data'] is! List) {
      return Center(
        child: Text(
          'No data available',
          style: GoogleFonts.poppins(color: Colors.grey),
        ),
      );
    }

    final rawData = data['Data'] as List<dynamic>;
    
    // Helper to safely get data and scale
    String getVal(int index, {double factor = 1.0, String unit = ''}) {
      if (index < rawData.length) {
        final val = (rawData[index] as num).toDouble() * factor;
        return '${val.toStringAsFixed(1)}$unit';
      }
      return '--';
    }

    return Scaffold(
      backgroundColor: const Color(0xFFF5F7FA),
      appBar: AppBar(
        title: Text('Data', style: GoogleFonts.poppins(color: Colors.black, fontWeight: FontWeight.bold)),
        centerTitle: true,
        backgroundColor: Colors.transparent,
        elevation: 0,
        automaticallyImplyLeading: false,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildExpandableSection(
            title: 'Solar',
            children: [
              _buildRow('PV1 Voltage', getVal(4, factor: 0.1, unit: ' V')),
              _buildRow('PV1 Current', getVal(1, factor: 0.1, unit: ' A')),
              _buildRow('PV1 Input Power', getVal(3, unit: ' W')),
              _buildRow('Total Solar Power', getVal(13, unit: ' W')),
            ],
          ),
          const SizedBox(height: 16),
          _buildExpandableSection(
            title: 'Grid',
            children: [
              _buildRow('Voltage', getVal(0, factor: 0.1, unit: ' V')),
              _buildRow('Current', getVal(8, factor: 0.1, unit: ' A')),
              _buildRow('Frequency', getVal(2, factor: 0.01, unit: ' Hz')),
              _buildRow('Grid Power', getVal(11, unit: ' W')),
              _buildRow('Consumed', '0.0 kWh'),
              _buildRow('Exported Energy', '0.0 kWh'),
            ],
          ),
          const SizedBox(height: 16),
          _buildExpandableSection(
            title: 'Inverter',
            children: [
              _buildRow('Active Power', getVal(3, unit: ' W')),
              _buildRow('Temperature', getVal(23, unit: ' °C')),
            ],
          ),
          const SizedBox(height: 16),
           _buildExpandableSection(
            title: 'Yield',
            children: [
              _buildRow('Daily Yield', getVal(21, factor: 0.1, unit: ' kWh')),
              _buildRow('Total Yield', getVal(19, factor: 0.1, unit: ' kWh')),
            ],
          ),
          const SizedBox(height: 16),
           _buildExpandableSection(
            title: 'About',
            children: [
               _buildRow('Inverter Serial', data['sn'] ?? '--'),
               _buildRow('Model/Info', (data['Information'] is List && (data['Information'] as List).length > 2) ? data['Information'][2].toString() : '--'),
               _buildRow('Firmware Version', data['ver'] ?? '--'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildExpandableSection({required String title, required List<Widget> children}) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Theme(
        data: ThemeData().copyWith(dividerColor: Colors.transparent),
        child: ExpansionTile(
          title: Text(
            title,
            style: GoogleFonts.poppins(
              fontSize: 16,
              fontWeight: FontWeight.w600,
              color: Colors.black87,
            ),
          ),
          childrenPadding: const EdgeInsets.only(left: 16, right: 16, bottom: 16),
          children: children,
        ),
      ),
    );
  }

  Widget _buildRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: GoogleFonts.poppins(color: Colors.grey[500], fontSize: 14),
          ),
          Text(
            value,
            style: GoogleFonts.poppins(
              color: Colors.black87,
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}
