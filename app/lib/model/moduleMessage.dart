class ModuleMessage {
  String command;
  dynamic message;
  int id;

  ModuleMessage({this.id, this.command, this.message});

  Map<String, dynamic> toJson() {
    return {'id': id, 'command': command, 'message': message};
  }

  factory ModuleMessage.fromJson(Map<String, dynamic> json) {
    return ModuleMessage(
        id: json['id'] as int,
        message: json['message'],
        command: json['command'] as String);
  }
}
