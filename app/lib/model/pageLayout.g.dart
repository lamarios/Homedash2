// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'pageLayout.dart';

// **************************************************************************
// JsonSerializableGenerator
// **************************************************************************

PageLayout _$PageLayoutFromJson(Map<String, dynamic> json) {
  return PageLayout(
    layout: json['layout'] == null
        ? null
        : Layout.fromJson(json['layout'] as Map<String, dynamic>),
    modules: (json['modules'] as List)
        ?.map((e) =>
            e == null ? null : ModuleLayout.fromJson(e as Map<String, dynamic>))
        ?.toList(),
  );
}

Map<String, dynamic> _$PageLayoutToJson(PageLayout instance) =>
    <String, dynamic>{
      'layout': instance.layout?.toJson(),
      'modules': instance.modules?.map((e) => e?.toJson())?.toList(),
    };
