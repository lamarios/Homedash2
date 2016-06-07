package ca.benow.transmission.model;

import org.json.JSONException;
import org.json.JSONObject;

public class AddedTorrentInfo extends JSONAccessor {

  public AddedTorrentInfo(JSONObject jsonObject) {
    super(jsonObject);
  }

  public int getId() throws JSONException {
    return obj.getInt("id");
  }

  public String getName() throws JSONException {
    return obj.getString("name");
  }

  public String getHashString() throws JSONException {
    return obj.getString("hashString");
  }

  @Override
  public String toString() {
    try {
		return obj.toString(2);
	} catch (JSONException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return null;
	}
  }
}
