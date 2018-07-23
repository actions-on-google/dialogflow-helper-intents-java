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

import com.google.actions.api.*;
import com.google.actions.api.response.ResponseBuilder;
import com.google.actions.api.response.systemintent.*;
import com.google.api.services.actions_fulfillment.v2.model.DateTime;
import com.google.api.services.actions_fulfillment.v2.model.Location;
import com.google.api.services.actions_fulfillment.v2.model.SimpleResponse;
import com.google.api.services.actions_fulfillment.v2.model.UserProfile;

import java.util.concurrent.CompletableFuture;

public class SystemIntentsApp extends DialogflowApp {

  private static final String[] SUGGESTIONS = new String[]{
          "confirmation",
          "date time",
          "permissions",
          "place",
          "sign in"
  };

  @ForIntent("Default Welcome Intent")
  public CompletableFuture<ActionResponse> welcome(ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    responseBuilder
            .add(new SimpleResponse()
                    .setDisplayText("Hello there")
                    .setTextToSpeech("Hi there!"))
            .add(new SimpleResponse()
                    .setTextToSpeech("I can show you confirmation, " +
                            "permission or location request on your phone.")
                    .setDisplayText("I can show you confirmation, " +
                            "permission, location request or sign in"))
            .addSuggestions(SUGGESTIONS);
    return CompletableFuture.completedFuture(responseBuilder.build());
  }

  @ForIntent("actions_intent_no_input")
  public CompletableFuture<ActionResponse> handleNoInput(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    int repromptCount = request.getRepromptCount() == null ? 0 :
            request.getRepromptCount().intValue();
    if (repromptCount == 0) {
      responseBuilder.add("What was that?").build();
    } else if (repromptCount == 1) {
      responseBuilder.add(
              "Sorry I didn't catch that. Could you please repeat?").build();
    } else if (request.isFinalPrompt()) {
      responseBuilder.add("Okay let's try this again later.")
              .endConversation()
              .build();
    }

    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  @ForIntent("askForConfirmation")
  public CompletableFuture<ActionResponse> askForConfirmation(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();

    responseBuilder
            .add("Placeholder for confirmation")
            .addSuggestions(SUGGESTIONS)
            .add(new Confirmation()
                    .setConfirmationText("Are you sure you want to do this?"));

    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  @ForIntent("actions_intent_confirmation")
  public CompletableFuture<ActionResponse> handleConfirmationResponse(
          ActionRequest request) {
    boolean userConfirmation = request.getUserConfirmation() != null &&
            request.getUserConfirmation().booleanValue();

    ResponseBuilder responseBuilder = getResponseBuilder();
    responseBuilder
            .add(userConfirmation ?
                    "Thank you for confirming" :
                    "No problem. We won't bother you")
            .addSuggestions(SUGGESTIONS);
    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  @ForIntent("askForDateTime")
  public CompletableFuture<ActionResponse> askForDateTime(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    responseBuilder
            .add("Placeholder for confirmation text")
            .add(new DateTimePrompt()
                    .setDateTimePrompt("When do you want to come in?")
                    .setDatePrompt("Which date works for you?")
                    .setTimePrompt("What time works for you?"));
    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  @ForIntent("actions_intent_datetime")
  public CompletableFuture<ActionResponse> handleDateTimeResponse(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    String response;
    DateTime dateTimeValue = request.getDateTime();

    if (dateTimeValue != null) {
      response = "Thank you for your response. We will see you on " +
              dateTimeValue.getDate();
    } else {
      response = "Sorry, I didn't get that.";
    }
    responseBuilder
            .add(response)
            .addSuggestions(SUGGESTIONS);
    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }


  @ForIntent("askForPermissions")
  public CompletableFuture<ActionResponse> askForPermission(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    responseBuilder
            .add("Placeholder for permissions text")
            .add(new Permission()
                    .setPermissions(new String[]{
                            ConstantsKt.PERMISSION_NAME,
                            ConstantsKt.PERMISSION_DEVICE_PRECISE_LOCATION
                    })
                    .setContext("To provide a better experience"));

    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  @ForIntent("actions_intent_permission")
  public CompletableFuture<ActionResponse> handlePermissionResponse(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    boolean havePermission = request.isPermissionGranted() != null &&
            request.isPermissionGranted().booleanValue();
    String response;
    if (havePermission) {
      UserProfile userProfile = request.getUser().getProfile();
      if (userProfile != null) {
        response = "Thank you, " + userProfile.getDisplayName() + ".";
      } else {
        response = "Thank you.";
      }
      Location location = request.getDevice().getLocation();
      if (location != null) {
        response += " We can find something near your location - " +
                getLocationString(location);
      }
    } else {
      response = "That's ok. We will still make it work.";
    }

    responseBuilder
            .add(response)
            .addSuggestions(SUGGESTIONS);

    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  private String getLocationString(Location location) {
    String formattedAddress = location.getFormattedAddress();
    if (formattedAddress != null) {
      return formattedAddress;
    } else if (location.getCity() != null) {
      return location.getCity();
    } else if (location.getCoordinates() != null) {
      return location.getCoordinates().getLatitude() + " / " +
              location.getCoordinates().getLongitude();
    }
    return "";
  }

  @ForIntent("askForPlace")
  public CompletableFuture<ActionResponse> askForPlace(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    responseBuilder
            .add("Placeholder for place text")
            .add(new Place()
                    .setRequestPrompt("Where do you want to have lunch?")
                    .setPermissionContext("To find lunch locations"));

    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  @ForIntent("actions_intent_place")
  public CompletableFuture<ActionResponse> handlePlaceResponse(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    Location location = request.getPlace();

    String response;
    if (location != null) {
      response = " Suggested place - " + getLocationString(location);
    } else {
      response = "Sorry, I need your location to suggest lunch places";
    }

    responseBuilder
            .add(response)
            .addSuggestions(SUGGESTIONS);

    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  @ForIntent("askForSignIn")
  public CompletableFuture<ActionResponse> askForSignIn(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();

    if (!request.hasCapability(Capability.SCREEN_OUTPUT.getValue())) {
      responseBuilder.add("Sign in is only available on devices " +
              "with a screen");
    } else {
      responseBuilder
              .add("Placeholder for sign in text")
              .add(new SignIn());
    }

    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }

  @ForIntent("actions_intent_sign_in")
  public CompletableFuture<ActionResponse> handleSignInResponse(
          ActionRequest request) {
    ResponseBuilder responseBuilder = getResponseBuilder();
    boolean signedIn = request.isSignedIn() != null &&
            request.isSignedIn().booleanValue();

    responseBuilder
            .add(signedIn ? "Successfully signed in" : "Unable to sign in")
            .addSuggestions(SUGGESTIONS);

    return CompletableFuture.completedFuture(
            responseBuilder.build());
  }
}
