import 'package:flutter/material.dart';
import '../services/api_service.dart';

class DataProvider with ChangeNotifier {
  final ApiService _apiService = ApiService();
  
  Map<String, dynamic>? _data;
  bool _isLoading = false;
  String? _error;
  String _ipAddress = '192.168.10.10';
  // Default password can be overridden at build time using --dart-define=DEFAULT_SOLAX_PWD=YourPwd
  String _password = const String.fromEnvironment('DEFAULT_SOLAX_PWD', defaultValue: '');

  Map<String, dynamic>? get data => _data;
  bool get isLoading => _isLoading;
  String? get error => _error;
  String get ipAddress => _ipAddress;
  String get password => _password;

  void setSettings(String ip, String pwd) {
    _ipAddress = ip;
    _password = pwd;
    notifyListeners();
  }

  Future<void> fetchData() async {
    _isLoading = true;
    _error = null;
    notifyListeners();

    try {
      _data = await _apiService.fetchRealTimeData(_ipAddress, _password);
    } catch (e) {
      _error = e.toString();
      _data = null;
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }
}
