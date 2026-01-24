import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  Future<Map<String, dynamic>> fetchRealTimeData(String ip, String pwd) async {
    final url = Uri.parse('http://$ip/');
    try {
      final response = await http.post(
        url,
        body: 'optType=ReadRealTimeData&pwd=$pwd',
        headers: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'X-Forwarded-For': '5.8.8.8',
        },
      ).timeout(const Duration(seconds: 10));

      if (response.statusCode == 200) {
        // SolaX sometimes returns malformed JSON or JSON wrapped in text.
        // We will try to decode it directly.
        try {
          return json.decode(response.body) as Map<String, dynamic>;
        } catch (e) {
          throw Exception('Failed to parse JSON: ${response.body}');
        }
      } else {
        throw Exception('HTTP Error: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Connection Error: $e');
    }
  }
}
