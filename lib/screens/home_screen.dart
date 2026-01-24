import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:provider/provider.dart';
import '../providers/data_provider.dart';
import 'overview_screen.dart';
import 'data_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 0;
  
  final List<Widget> _screens = [
    const OverviewScreen(),
    const Center(child: Text('Alarm Screen Placeholder')), // Alarm
    const DataScreen(),
  ];

  @override
  void initState() {
    super.initState();
    Future.microtask(() =>
        Provider.of<DataProvider>(context, listen: false).fetchData());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [Color(0xFFE3F2FD), Color(0xFFFAFAFA)],
            stops: [0.0, 0.3],
          ),
        ),
        child: SafeArea(
          child: _screens[_currentIndex],
        ),
      ),
      bottomNavigationBar: BottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: (index) {
          setState(() {
            _currentIndex = index;
          });
        },
        backgroundColor: Colors.white,
        selectedItemColor: Colors.black87,
        unselectedItemColor: Colors.grey,
        showUnselectedLabels: true,
        selectedLabelStyle: GoogleFonts.poppins(fontSize: 12, fontWeight: FontWeight.w500),
        unselectedLabelStyle: GoogleFonts.poppins(fontSize: 12),
        type: BottomNavigationBarType.fixed,
        items: const [
          BottomNavigationBarItem(
            icon: Icon(Icons.home_outlined),
            activeIcon: Icon(Icons.home),
            label: 'Overview',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.error_outline),
            activeIcon: Icon(Icons.error),
            label: 'Alarm',
          ),
          BottomNavigationBarItem(
            icon: Icon(Icons.grid_view),
            activeIcon: Icon(Icons.grid_view_rounded),
            label: 'Data',
          ),
        ],
      ),
    );
  }
}
