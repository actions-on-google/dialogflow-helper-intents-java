/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.google.actions.api.ActionRequest;
import com.google.actions.api.ActionResponse;
import com.google.actions.api.Capability;
import com.google.actions.api.ConstantsKt;
import com.google.actions.api.DialogflowApp;
import com.google.actions.api.ForIntent;
import com.google.actions.api.response.ResponseBuilder;
import com.google.actions.api.response.helperintent.Confirmation;
import com.google.actions.api.response.helperintent.DateTimePrompt;
import com.google.actions.api.response.helperintent.Permission;
import com.google.actions.api.response.helperintent.Place;
import com.google.actions.api.response.helperintent.SignIn;
import com.google.api.services.actions_fulfillment.v2.model.DateTime;
import com.google.api.services.actions_fulfillment.v2.model.Location;
import com.google.api.services.actions_fulfillment.v2.model.SimpleResponse;
import com.google.api.services.actions_fulfillment.v2.model.UserProfile;
import java.text.MessageFormat;
import java.util.ResourceBundle;

public class HelperIntentsApp extends DialogflowApp {

  private static final String[] SUGGESTIONS =
      new String[]{"Confirmation", "DateTime", "Permissions", "Place", "Sign In"};

  @ForIntent("Default Welcome Intent")
  public ActionResponse welcome(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    responseBuilder
        .add(
            new SimpleResponse()
                .setDisplayText(rb.getString("welcome_1"))
                .setTextToSpeech(rb.getString("welcome_1")))
        .add(
            new SimpleResponse()
                .setTextToSpeech(rb.getString("options_tts"))
                .setDisplayText(rb.getString("options_speech")))
        .addSuggestions(SUGGESTIONS);
    return responseBuilder.build();
  }

  @ForIntent("actions_intent_no_input")
  public ActionResponse handleNoInput(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());
    int repromptCount = request.getRepromptCount() == null ? 0 : request.getRepromptCount();
    if (repromptCount == 0) {
      responseBuilder.add(rb.getString("no_input_1")).build();
    } else if (repromptCount == 1) {
      responseBuilder.add(rb.getString("no_input_2")).build();
    } else if (request.isFinalPrompt()) {
      responseBuilder.add(rb.getString("no_input_3")).endConversation().build();
    }

    return responseBuilder.build();
  }

  @ForIntent("askForConfirmation")
  public ActionResponse askForConfirmation(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    responseBuilder
        .add(rb.getString("conf_placeholder"))
        .addSuggestions(SUGGESTIONS)
        .add(new Confirmation().setConfirmationText(rb.getString("conf_text")));

    return responseBuilder.build();
  }

  @ForIntent("actions_intent_confirmation")
  public ActionResponse handleConfirmationResponse(ActionRequest request) {
    boolean userConfirmation = request.getUserConfirmation();

    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    responseBuilder
        .add(
            userConfirmation
                ? rb.getString("conf_response_success")
                : rb.getString("conf_response_failure"))
        .addSuggestions(SUGGESTIONS);
    return responseBuilder.build();
  }

  @ForIntent("askForDateTime")
  public ActionResponse askForDateTime(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    ResponseBuilder add = responseBuilder
            .add(rb.getString("datetime_placeholder"))
            .add(
                    new DateTimePrompt()
                            .setDateTimePrompt(rb.getString("datetime_prompt_init"))
                            .setDatePrompt("datetime_date_prompt")
                            .setTimePrompt(rb.getString("datetime_time_prompt")));
    return responseBuilder.build();
  }

  @ForIntent("actions_intent_datetime")
  public ActionResponse handleDateTimeResponse(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    String response;
    DateTime dateTimeValue = request.getDateTime();

    if (dateTimeValue != null) {
      Integer minutes = dateTimeValue.getTime().getMinutes();
      String minutesStr = (minutes != null) ? String.valueOf(minutes) : "00";
        response =
            MessageFormat.format(
                rb.getString("datetime_response_success"),
                dateTimeValue.getDate().getDay(),
                dateTimeValue.getDate().getMonth(),
                dateTimeValue.getTime().getHours(),
                minutesStr);
    } else {
      response = rb.getString("datetime_response_failure");
    }
    responseBuilder.add(response).addSuggestions(SUGGESTIONS);
    return responseBuilder.build();
  }

  @ForIntent("askForPermissions")
  public ActionResponse askForPermission(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    String[] permissions = new String[]{ConstantsKt.PERMISSION_NAME};
    if (request.getUser().getUserVerificationStatus().equals("VERIFIED")) {
      // Location permissions only work for verified users
      // https://developers.google.com/actions/assistant/guest-users
      permissions = new String[]{
        ConstantsKt.PERMISSION_NAME, ConstantsKt.PERMISSION_DEVICE_PRECISE_LOCATION
      };
    }
    responseBuilder
        .add(rb.getString("permission_placeholder"))
        .add(
            new Permission()
                .setPermissions(permissions)
                .setContext(rb.getString("permission_context")));

    return responseBuilder.build();
  }

  @ForIntent("actions_intent_permission")
  public ActionResponse handlePermissionResponse(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    boolean havePermission = request.isPermissionGranted();
    String response;
    if (havePermission) {
      UserProfile userProfile = request.getUser().getProfile();
      if (userProfile != null) {
        response =
            MessageFormat.format(
                rb.getString("permission_response_success_1"), userProfile.getDisplayName());
      } else {
        response = rb.getString("permission_response_success_2");
      }
      Location location = request.getDevice().getLocation();
      if (location != null) {
        response +=
            MessageFormat.format(
                rb.getString("permission_response_location_1"), getLocationString(location));
      }
    } else {
      response = rb.getString("permission_response_location_2");
    }

    responseBuilder.add(response).addSuggestions(SUGGESTIONS);

    return responseBuilder.build();
  }

  private String getLocationString(Location location) {
    String formattedAddress = location.getFormattedAddress();
    if (formattedAddress != null) {
      return formattedAddress;
    } else if (location.getCity() != null) {
      return location.getCity();
    } else if (location.getCoordinates() != null) {
      return location.getCoordinates().getLatitude()
          + " / "
          + location.getCoordinates().getLongitude();
    }
    return "";
  }

  @ForIntent("askForPlace")
  public ActionResponse askForPlace(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    responseBuilder
        .add(rb.getString("place_placeholder"))
        .add(
            new Place()
                .setRequestPrompt(rb.getString("place_prompt"))
                .setPermissionContext(rb.getString("place_context")));

    return responseBuilder.build();
  }

  @ForIntent("actions_intent_place")
  public ActionResponse handlePlaceResponse(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());
    Location location = request.getPlace();

    String response;
    if (location != null) {
      response =
          MessageFormat.format(rb.getString("place_response_success"), getLocationString(location));
    } else {
      response = rb.getString("place_response_failure");
    }

    responseBuilder.add(response).addSuggestions(SUGGESTIONS);

    return responseBuilder.build();
  }

  @ForIntent("askForSignIn")
  public ActionResponse askForSignIn(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());
    if (!request.getUser().getUserVerificationStatus().equals("VERIFIED")) {
      // Account linking only works for verified users
      // https://developers.google.com/actions/assistant/guest-users
      responseBuilder.add(rb.getString("signin_placeholder_guest_error"));
    } else {
      responseBuilder.add(rb.getString("signin_placeholder")).add(new SignIn());
    }

    return responseBuilder.build();
  }

  @ForIntent("actions_intent_sign_in")
  public ActionResponse handleSignInResponse(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder(request);
    ResourceBundle rb = ResourceBundle.getBundle("resources", request.getLocale());

    boolean signedIn = request.isSignInGranted();

    responseBuilder
        .add(
            signedIn
                ? rb.getString("signin_response_success")
                : rb.getString("signin_response_failure"))
        .addSuggestions(SUGGESTIONS);

    return responseBuilder.build();
  }
}
