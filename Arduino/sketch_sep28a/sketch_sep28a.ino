#include <SoftwareSerial.h>

SoftwareSerial bluetooth(2, 3); // RX, TX

const int TRIG_PIN = 9;
const int ECHO_PIN = 10;

const float DISTANCIA_MINIMA = 3.0;
const float DISTANCIA_MAXIMA = 20.0;

bool objetoPresente = false;

float medirDistancia() {
  digitalWrite(TRIG_PIN, LOW);
  delayMicroseconds(3);

  digitalWrite(TRIG_PIN, HIGH);
  delayMicroseconds(10);
  digitalWrite(TRIG_PIN, LOW);

  unsigned long duracion =
      pulseIn(ECHO_PIN, HIGH, 30000UL);

  if (duracion == 0) {
    return -1;
  }

  return duracion * 0.0343 / 2.0;
}

void setup() {
  Serial.begin(9600);
  bluetooth.begin(9600);

  pinMode(TRIG_PIN, OUTPUT);
  pinMode(ECHO_PIN, INPUT);

  digitalWrite(TRIG_PIN, LOW);

  Serial.println("FoxLift iniciado");
  bluetooth.println("FOX LIFT LISTO");
}

void loop() {
  float distancia = medirDistancia();

  bool detectado =
      distancia >= DISTANCIA_MINIMA &&
      distancia <= DISTANCIA_MAXIMA;

  if (detectado && !objetoPresente) {
    Serial.print("Objeto detectado: ");
    Serial.print(distancia, 2);
    Serial.println(" cm");

    bluetooth.print("OBJETO:");
    bluetooth.println(distancia, 2);
  }

  objetoPresente = detectado;

  delay(200);
}