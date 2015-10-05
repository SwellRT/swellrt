package org.swellrt.server.box.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface SwellRTService {

  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException;

}
