package ca.benow.transmission;

import java.io.IOException;

/**
 * Indicates the problem transmission had when processing of a request.
 * 
 * @author andy
 *
 */
public class TransmissionException extends IOException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;
  public final String responseText;
  private final String requestText;

  public TransmissionException(String msg, String requestText, String responseText) {
    super(msg);
    this.requestText = requestText;
    this.responseText = responseText;
  }

}
