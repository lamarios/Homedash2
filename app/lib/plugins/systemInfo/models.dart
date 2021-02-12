class Refresh {
  List<CpuInfo> cpuInfo;
  List<RamInfo> ramInfo;

  Refresh({this.cpuInfo, this.ramInfo});

  factory Refresh.fromJson(Map<String, dynamic> json) {
    return Refresh(
        cpuInfo:
            (json['cpuInfo'] as List).map((e) => CpuInfo.fromJson(e)).toList(),
        ramInfo:
            (json['ramInfo'] as List).map((e) => RamInfo.fromJson(e)).toList());
  }
}

class CpuInfo {
  double cpuUsage;
  double temperature;
  double voltage;
  List<int> fanSpeed;
  List<double> coreUsage;

  CpuInfo(
      {this.cpuUsage,
      this.temperature,
      this.voltage,
      this.fanSpeed,
      this.coreUsage});

  factory CpuInfo.fromJson(Map<String, dynamic> json) {
    return CpuInfo(
      cpuUsage: json['cpuUsage'] as double,
      temperature: json['temperature'] as double,
      voltage: json['voltage'] as double,
      fanSpeed: (json['fanSpeed'] as List).map((e) => e as int).toList(),
      coreUsage: (json['coreUsage'] as List).map((e) => e as double).toList()
    );
  }
}

class RamInfo {
  int maxRam;
  int availableRam;
  int usedRam;
  double percentageUsed;

  RamInfo({this.maxRam, this.availableRam, this.usedRam, this.percentageUsed});

  factory RamInfo.fromJson(Map<String, dynamic> json) {
    return RamInfo(
        maxRam: json['maxRam'] as int,
        availableRam: json['availableRam'] as int,
        usedRam: json['usedRam'] as int,
        percentageUsed: json['percentageUsed'] as double);
  }
}
