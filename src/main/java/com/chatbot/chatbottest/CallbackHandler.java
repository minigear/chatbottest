package com.chatbot.chatbottest;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.exception.MessengerApiException;
import com.github.messenger4j.exception.MessengerIOException;
import com.github.messenger4j.exception.MessengerVerificationException;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.message.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/webhook")
public class CallbackHandler {
    private static final Logger logger = LoggerFactory.getLogger(CallbackHandler.class);

    private static final String RESOURCE_URL =
            "https://raw.githubusercontent.com/fbsamples/messenger-platform-samples/master/node/public";
    public static final String GOOD_ACTION = "DEVELOPER_DEFINED_PAYLOAD_FOR_GOOD_ACTION";
    public static final String NOT_GOOD_ACTION = "DEVELOPER_DEFINED_PAYLOAD_FOR_NOT_GOOD_ACTION";

    /**
     * Constructs the {@code CallBackHandler} and initializes the {@code MessengerReceiveClient}.
     *
     * @param appSecret   the {@code Application Secret}
     * @param verifyToken the {@code Verification Token} that has been provided by you during the setup of the {@code
     *                    Webhook}
     */

    @Autowired
    public CallbackHandler(@Value("${messenger4j.appSecret}") final String appSecret,
                           @Value("${messenger4j.verifyToken}") final String verifyToken
    ) {
        logger.debug("Initializing MessengerReceiveClient - appSecret: {} | verifyToken: {}", appSecret, verifyToken);
    }

    /**
     * Webhook verification endpoint.
     * <p>
     * The passed verification token (as query parameter) must match the configured verification token.
     * In case this is true, the passed challenge string must be returned by this endpoint.
     */
    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") final String mode,
                                                @RequestParam("hub.verify_token") final String verifyToken,
                                                @RequestParam("hub.challenge") final String challenge) throws MessengerVerificationException, MessengerApiException, MessengerIOException {

        logger.debug("Received Webhook verification request - mode: {} | verifyToken: {} | challenge: {}", mode,
                verifyToken, challenge);

        Messenger messenger = Messenger.create("", "", "VERIFY_TOKEN");

        try {
            messenger.verifyWebhook(mode, verifyToken);
        } catch (MessengerVerificationException e) {
            System.out.println("not verify token");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
        System.out.printf("ok");
        return ResponseEntity.ok("");
    }

    /**
     * Callback endpoint responsible for processing the inbound messages and events.
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<Void> handleCallback(@RequestBody final String payload,
                                               @RequestHeader("X-Hub-Signature") final String signature) throws MessengerVerificationException {

        logger.debug("Received Messenger Platform callback - payload: {} | signature: {}", payload, signature);
        //            this.receiveClient.processCallbackPayload(payload, signature);
        logger.debug("Processed callback payload successfully");

        Messenger messenger = Messenger.create("", "", "");

        messenger.onReceiveEvents(payload, Optional.empty(), event -> {
            final String senderId = event.senderId();
            if (event.isTextMessageEvent()) {
                final String text = event.asTextMessageEvent().text();

                final TextMessage textMessage = TextMessage.create(text);
                final MessagePayload messagePayload = MessagePayload.create(senderId, textMessage);

                try {
                    messenger.send(messagePayload);
                } catch (MessengerApiException | MessengerIOException e) {
                    // Oops, something went wrong
                }
            }
        });

        return ResponseEntity.status(HttpStatus.OK).build();
    }


    private void handleSendException(Exception e) {
        logger.error("Message could not be sent. An unexpected error occurred.", e);
    }

    private void handleIOException(Exception e) {
        logger.error("Could not open Spring.io page. An unexpected error occurred.", e);
    }
}
