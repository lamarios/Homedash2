import 'dart:async';

import 'package:app/model/moduleMessage.dart';
import 'package:app/plugins/systemInfo/models.dart';
import 'package:app/widgets/module.dart';
import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';

class SystemInfo extends ModuleWidget {
  @override
  State<StatefulWidget> createState() => SystemInfoState();

  SystemInfo(
      {Key key, StreamController<ModuleMessage> stream, int width, int height})
      : super(key: key, stream: stream, width: width, height: height);
}

class SystemInfoState extends ModuleWidgetState<SystemInfo> {
  List<FlSpot> getCpuData(List<CpuInfo> data, int maxData) {
    List<FlSpot> spots = [];
    spots.length = maxData;
    for (int i = maxData - 1; i >= 0; i--) {
      int cpuIndex = data.length - (maxData - i);
      if (cpuIndex >= 0) {
        spots[i] = FlSpot(i.toDouble(), data[cpuIndex].cpuUsage);
      } else {
        spots[i] = FlSpot(i.toDouble(), 0);
      }
    }
    return spots;
  }

  List<FlSpot> getRamData(List<RamInfo> data, int maxData) {
    List<FlSpot> spots = [];
    spots.length = maxData;
    for (int i = maxData - 1; i >= 0; i--) {
      int cpuIndex = data.length - (maxData - i);
      if (cpuIndex >= 0) {
        spots[i] = FlSpot(i.toDouble(), data[cpuIndex].percentageUsed);
      } else {
        spots[i] = FlSpot(i.toDouble(), 0);
      }
    }
    return spots;
  }

  @override
  Widget build(BuildContext context) {
    print('stream ${widget.stream == null} ${widget.stream.stream == null}');

    var color = Color.fromRGBO(16, 207, 189, 1.0);

    return Container(
        color: color,
        child: StreamBuilder(
            stream: widget.stream.stream,
            builder: (context, snapshot) {
              if (snapshot.hasData) {
                Refresh refresh = Refresh.fromJson(snapshot.data.message);
                return Stack(children: [
                  LineChart(
                    LineChartData(
                        maxY: 101,
                        backgroundColor: color,
                        titlesData: FlTitlesData(show: false),
                        gridData: FlGridData(show: false),
                        borderData: FlBorderData(show: false),
                        lineTouchData: LineTouchData(enabled: false),
                        lineBarsData: [
                          LineChartBarData(
                              isCurved: false,
                              barWidth: 1,
                              spots: getRamData(refresh.ramInfo, 20),
                              colors: [Color.fromRGBO(10, 127, 116, 0.2)],
                              belowBarData: BarAreaData(show: true, colors:[Color.fromRGBO(10, 127, 116, 0.2)]),
                              dotData: FlDotData(show: false)),
                          LineChartBarData(
                              isCurved: false,
                              barWidth: 1,
                              spots: getCpuData(refresh.cpuInfo, 20),
                              colors: [Color.fromRGBO(5, 64, 58, 0.2)],
                              belowBarData: BarAreaData(show: true, colors:[Color.fromRGBO(5, 64, 58, 0.2)]),
                              dotData: FlDotData(show: false))
                        ]),
                    swapAnimationDuration: Duration(milliseconds: 0),
                  ),
                  Padding(
                      padding: EdgeInsets.all(10),
                      child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          crossAxisAlignment: CrossAxisAlignment.center,
                          children: [
                            RichText(
                                text: TextSpan(children: [
                              TextSpan(
                                  text: 'CPU: ',
                                  style: TextStyle(
                                      color:
                                          Color.fromRGBO(255, 255, 255, 0.8))),
                              TextSpan(
                                text: refresh
                                        .cpuInfo[refresh.cpuInfo.length - 1]
                                        .cpuUsage
                                        .toString() +
                                    '%',
                                style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                              )
                            ])),
                            RichText(
                                text: TextSpan(children: [
                              TextSpan(
                                  text: 'RAM: ',
                                  style: TextStyle(
                                      color:
                                          Color.fromRGBO(255, 255, 255, 0.8))),
                              TextSpan(
                                text: refresh
                                        .ramInfo[refresh.ramInfo.length - 1]
                                        .percentageUsed
                                        .toString() +
                                    '%',
                                style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
                              )
                            ])),
                          ]))
                ]);
              } else {
                return Center(
                    heightFactor: 1.0,
                    child: CircularProgressIndicator(
                      backgroundColor: Colors.white,
                    ));
              }
            }));
  }
}
