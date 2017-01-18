package dev;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.Node;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;

import org.w3c.dom.Element;

public class SoapRequest {
	public static void main(String[] args) throws Exception {
		SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
		SOAPConnection soapConnection = soapConnectionFactory.createConnection();

		// Send SOAP Message to SOAP Server
		String url = "http://ws.cdyne.com/emailverify/Emailvernotestemail.asmx";
		SOAPMessage soapResponse = soapConnection.call(createSOAPRequest(), url);

		// print SOAP Response
		System.out.println("Response SOAP Message:");
		soapResponse.writeTo(System.out);
		System.out.println();

		Node nodes = (Node) soapResponse.getSOAPBody().getChildElements().next();
		Element eles = (Element) nodes;
		System.out.println(eles.getFirstChild().getChildNodes().getLength());

		int childLength = eles.getFirstChild().getChildNodes().getLength();
		for (int i = 0; i < childLength; i++) {
			System.out.println(eles.getFirstChild().getChildNodes().item(i).getTextContent());
		}

		soapConnection.close();
	}

	private static SOAPMessage createSOAPRequest() throws Exception {
		MessageFactory messageFactory = MessageFactory.newInstance();
		SOAPMessage soapMessage = messageFactory.createMessage();
		SOAPPart soapPart = soapMessage.getSOAPPart();

		String serverURI = "http://ws.cdyne.com/";

		// SOAP Envelope
		SOAPEnvelope envelope = soapPart.getEnvelope();
		envelope.addNamespaceDeclaration("example", serverURI);

		/*
		 * Constructed SOAP Request Message: <SOAP-ENV:Envelope
		 * xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
		 * xmlns:example="http://ws.cdyne.com/"> <SOAP-ENV:Header/>
		 * <SOAP-ENV:Body> <example:VerifyEmail>
		 * <example:email>mutantninja@gmail.com</example:email>
		 * <example:LicenseKey>123</example:LicenseKey> </example:VerifyEmail>
		 * </SOAP-ENV:Body> </SOAP-ENV:Envelope>
		 */

		// SOAP Body
		SOAPBody soapBody = envelope.getBody();
		SOAPElement soapBodyElem = soapBody.addChildElement("VerifyEmail", "example");
		SOAPElement soapBodyElem1 = soapBodyElem.addChildElement("email", "example");
		soapBodyElem1.addTextNode("diastowo@gmail.com");
		SOAPElement soapBodyElem2 = soapBodyElem.addChildElement("LicenseKey", "example");
		soapBodyElem2.addTextNode("1qwe23");

		MimeHeaders headers = soapMessage.getMimeHeaders();
		headers.addHeader("SOAPAction", serverURI + "VerifyEmail");

		soapMessage.saveChanges();

		/* Print the request message */
		System.out.print("Request SOAP Message:");
		soapMessage.writeTo(System.out);
		System.out.println();

		return soapMessage;
	}
}
