
class A {}
class B extends A {}
class C {
  static java.util.Collection<B> getBs() {
    return java.util.Collections.singleton(new B());
  }
  static java.util.Collection<? extends B> getExtendedBs() {
    return java.util.Collections.singleton(new B());
  }
  static java.util.Collection<? extends A> getExtendedAs() {
    return java.util.Collections.singleton(new B());
  }
}
class D {
  class E {}
  static java.util.Collection<E> getEs() {
    return java.util.Collections.singleton(new E());
  }
}
public class CheckForLoop {
  void doStuff() {
    java.util.Collection unparameterized = java.util.Collections.emptySet();
    for (Object o: unparameterized) {}

    java.util.List<B> listOfB = java.util.Collections.singletonList(new B());
    for (B b: listOfB) {}
    for (A a: listOfB) {}
    for (A a: listOfB) { // Noncompliant [[sc=10;ec=11;secondary=29]] {{Change "A" to the type handled by the Collection.}}
      (B) a;
    }
    for (Object o: listOfB) {}
    for (Object o: listOfB) { // Noncompliant {{Change "Object" to the type handled by the Collection.}}
      (B) o;
    }

    for (B b: C.getBs()) {}
    for (A b: C.getBs()) {}
    for (A a: C.getBs()) { // Noncompliant {{Change "A" to the type handled by the Collection.}}
      (B) a;
    }

    for (B b: C.getExtendedBs()) {}
    for (A a: C.getExtendedBs()) {}
    for (A a: C.getExtendedBs()) { // Noncompliant {{Change "A" to the type handled by the Collection.}}
      (B) a;
    }
    for (A a: C.getExtendedAs()) {}

    for (B b: java.util.Collections.singletonList(new B())) {}
    for (A a: java.util.Collections.singletonList(new B())) {}
    for (A a: java.util.Collections.singletonList(new B())) { // Noncompliant {{Change "A" to the type handled by the Collection.}}
      (B) a;
    }

    for (D.E e: D.getEs()) {}
    for (Object o; listOfB.hasNext(); o = listOfB.next()) {}
    while (listOfB.hasNext()) { listOfB.next(); }

    java.util.Set<java.util.Set<B>> setOfSetOfB = java.util.Collections.emptySet();
    for (java.util.Set<B> s: setOfSetOfB) {}
    for (java.util.Set s: setOfSetOfB) {}
    for (Object s: setOfSetOfB) {}
    for (Object s: setOfSetOfB) { // Noncompliant
      (B) s;
    }

    java.util.Map t = new java.util.HashMap();
    for (java.util.Map.Entry e : ((java.util.Map<?,?>)t).entrySet()) {}
  }
}
class MyMap<K, V> extends java.util.AbstractMap<K, V> {
  @Override
  void putAll(java.util.Map<? extends K, ? extends V> m) {
    for (Entry<? extends K, ? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }
}


class I {}
class J extends I {}
class K extends J {}
class L extends I {}

class Test {
  java.util.Collection<K> collectionOfK;
  java.util.Collection<J> collectionOfJ;
  java.util.Collection<I> collectionOfI;
  Object other;
  void doStuff() {
    for(K k: collectionOfK) {}
    for(J k: collectionOfK) {}
    for(I i: collectionOfK) {}
    for(J j: collectionOfK) { // Noncompliant
      (K) j;
    }
    for(J j: collectionOfK) {
      (K) other;
    }
    for(I i: collectionOfK) { // Noncompliant
      (J) i;
      (K) i;
    }
    for(I i: collectionOfK) { // Noncompliant
      (L) i;
    }
    for(J j: collectionOfJ) {
      (K) j;
    }
    for(I i: collectionOfJ) { // Noncompliant
      (J) i;
    }
    for(J j: collectionOfJ) {
      if (j instanceof K) {
        (K) j;
      }
    }
    for(I i: collectionOfI) {
      if (i instanceof K) {
        (K) i;
      }
    }
  }
}
