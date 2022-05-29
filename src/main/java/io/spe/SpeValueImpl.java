package io.spe;

final class SpeValueImpl {
    record Int(int value) implements SpeValue.Int {
        @Override
        public int get() {
            return value;
        }
    }
}
