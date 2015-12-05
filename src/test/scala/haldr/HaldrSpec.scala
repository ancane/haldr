package haldr

import org.specs2.mutable.Specification
import org.specs2.matcher.DataTables
import org.specs2.specification.Fragments
import spray.http.Uri._
import spray.json._
import DefaultJsonProtocol._
import haldr._

class HaldrSpec extends Specification {

  case class Customer(email: String)
  case class Address(street: String, house: Int)
  case class Car(make: String, model: String)
  case class Location(lng: Double, lat: Double)

  implicit val customerF = jsonFormat1(Customer)
  implicit val addressF  = jsonFormat2(Address)
  implicit val carF  = jsonFormat2(Car)
  implicit val locationF = jsonFormat2(Location)

  "HALdr" should {

    "render" in {

      "empty resource" >> {
        val customer = Resource()
        customer.toJson must_== "{}".parseJson
      }

      "self relation" >> {
        val customer = Resource(Map("email" -> "test@gmail.com"),
          u"/customers" / "dead / beaf")

        customer.toJson must_== """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {"href": "/customers/dead%20%2F%20beaf"}
            }
        }
        """.parseJson
      }

      "combined URIs" >> {
        val expected = """
        {
           "_links": {
              "self": {"href": "/customers/deadbeaf"}
            }
        }
        """.parseJson

        val customerUri = r"/customers/deadbeaf"

        Resource(customerUri).toJson must_== expected
        Resource(u"/customers" / r"/deadbeaf").toJson must_== expected
        Resource(u"/customers" / "deadbeaf").toJson must_== expected
        Resource(r"/customers" / "deadbeaf").toJson must_== expected
        Resource(r"/customers" / r"/deadbeaf").toJson must_== expected
      }

      "URIs with URL params" >> {
        val expected = """
        {
           "_links": {
              "self": {"href": "/customers/deadbeaf?dead=beaf"}
            }
        }
        """.parseJson

        Resource(r"/customers/deadbeaf"       q ("dead", "beaf")).toJson must_== expected
        Resource(u"/customers" / r"/deadbeaf" q ("dead", "beaf")).toJson must_== expected
        Resource(u"/customers" / "deadbeaf"   q ("dead", "beaf")).toJson must_== expected
        Resource(r"/customers" / "deadbeaf"   q ("dead", "beaf")).toJson must_== expected
        Resource(r"/customers" / r"/deadbeaf" q ("dead", "beaf")).toJson must_== expected
      }

      "URIs query param encoding" >> {
        val expected = """
        {
           "_links": {
              "self": {"href": "/customers/deadbeaf?dead=be+af"}
            }
        }
        """.parseJson

        Resource(r"/customers/deadbeaf" q ("dead", "be af")).toJson must_== expected
      }

      "additional link properties" >> {
        val customer = Resource(u"/customers/deadbeaf".prop("title", "John"))

        customer.toJson must_== """
        {
           "_links": {
              "self": {
                  "href": "/customers/deadbeaf",
                  "title": "John"
              }
            }
        }
        """.parseJson
      }

      "uri template" >> {
        val customer =
          Resource(Map("email" -> "test@gmail.com"), u"/customers/deadbeaf")
            .link("address", r"/address")
            .link("cars", r"/cars")
            .link("by-name", t"/customers?name={name}")

        customer.toJson must_== """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {"href": "/customers/deadbeaf"},
              "address": {"href": "/customers/deadbeaf/address"},
              "cars": {"href": "/customers/deadbeaf/cars"},
              "by-name": {
                  "href": "/customers?name={name}",
                  "templated": true
               }
            }
        }
        """.parseJson
      }

      "uri template with encoded parts" >> {
        val book = "Slash / Book"
        val customer =
          Resource(Map("email" -> "test@gmail.com"), u"/customers/deadbeaf")
            .link("book", t"/books/$book{?format}")

        customer.toJson must_== """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {"href": "/customers/deadbeaf"},
              "book": {
                  "href": "/books/Slash%20%2F%20Book{?format}",
                  "templated": true
               }
            }
        }
        """.parseJson
      }

      "multiple relations" >> {
        val customer =
          Resource(
            Customer("test@gmail.com"),
            u"/customers/deadbeaf"
              .prop("name", "John")
              .prop("type", "application/json"))
            .link("books", u"/customers/deadbeaf/books")
            .link("mailing-lists", u"/customers/deadbeaf/mailing-lists")

        customer.toJson must_== """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {
                  "href": "/customers/deadbeaf",
                  "name": "John",
                  "type": "application/json"
              },
              "books": {"href": "/customers/deadbeaf/books"},
              "mailing-lists": {"href": "/customers/deadbeaf/mailing-lists"}
            }
        }
        """.parseJson
      }

      "string links with url encoding and props" >> {
        val book = "Slash / Book".enc

        val customer =
          Resource(
            Customer("test@gmail.com"), u"/customers/deadbeaf")
            .link("book", s"/books/$book".prop("type", "fantasy"))

        customer.toJson must_== """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {"href": "/customers/deadbeaf"},
              "book": {
                 "href": "/books/Slash%20%2F%20Book",
                 "type": "fantasy"
              }
            }
        }
        """.parseJson
      }

      "spray's Path links with url encoding and props" >> {
        val customer =
          Resource(
            Customer("test@gmail.com"), u"/customers/deadbeaf")
            .link("books", Path / "books" )
            .link("cars", Path / "cars" prop("title", "Cars"))

        customer.toJson must_== """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {"href": "/customers/deadbeaf"},
              "books": {"href": "/books"},
              "cars": {
                 "href": "/cars",
                 "title": "Cars"
              }
            }
        }
        """.parseJson
      }

      "multiple relative links" >> {
        val customer =
          Resource(
            Customer("test@gmail.com"), u"/customers/deadbeaf")
            .link("books", r"/books")
            .link("mailing-lists", r"/mailing-lists")

        customer.toJson must_== """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {"href": "/customers/deadbeaf"},
              "books": {"href": "/customers/deadbeaf/books"},
              "mailing-lists": {"href": "/customers/deadbeaf/mailing-lists"}
            }
        }
        """.parseJson
      }

      "multiple links with same name" >> {
        val customer =
          Resource(
            Customer("test@gmail.com"), u"/customers/deadbeaf")
            .link("books", r"/books/1")
            .link("books", r"/books/2")
            .link("books", r"/books/3", r"/books/4")

        customer.toJson must_==
          """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {"href": "/customers/deadbeaf"},
              "books": [
                {"href": "/customers/deadbeaf/books/1"},
                {"href": "/customers/deadbeaf/books/2"},
                {"href": "/customers/deadbeaf/books/3"},
                {"href": "/customers/deadbeaf/books/4"}
              ]
            }
        }
          """.parseJson
      }

      "relative DRY links" >> {
        val customer =
          Resource(
            Customer("test@gmail.com"), u"/customers/deadbeaf")
            .link("books", r"/books")
            .link("mailing-groups", r"/mailing-lists/deadcafe/groups")

        customer.toJson must_== """
        {
           "email": "test@gmail.com",
           "_links": {
              "self": {"href": "/customers/deadbeaf"},
              "books": {"href": "/customers/deadbeaf/books"},
              "mailing-groups": {"href": "/customers/deadbeaf/mailing-lists/deadcafe/groups"}
            }
        }
        """.parseJson
      }
    }

    "embed related" in {

      "property" >> {
        val customer =
          Resource(
            Customer("test@gmail.com"),
            u"/customers/deadbeaf")
            .embed("address", Address("Argonautenstraat", 3))

        customer.toJson must_== """
        {
          "email": "test@gmail.com",
          "_embedded": {
             "address": {
                "street": "Argonautenstraat",
                "house": 3
             }
          },
          "_links": {
             "self": {"href": "/customers/deadbeaf"}
          }
        }
        """.parseJson
      }

      "resource" >> {
        val customer =
          Resource(Customer("test@gmail.com"), u"/customers/deadbeaf")
            .embed("address",
              Resource(Address("Argonautenstraat", 3), u"/customers/deadbeaf/address"))
            .link("books", u"/customers/deadbeaf/books")

        customer.toJson must_== """
        {
          "email": "test@gmail.com",
          "_embedded": {
             "address": {
                "street": "Argonautenstraat",
                "house": 3,
                "_links": {
                   "self": {"href": "/customers/deadbeaf/address"}
                }
             }
          },
          "_links": {
             "self": {"href": "/customers/deadbeaf"},
             "books": {"href": "/customers/deadbeaf/books"}
          }
        }
        """.parseJson
      }

      "collection property" >> {
        val customer =
          Resource(Customer("test@gmail.com"), u"/customers/deadbeaf")
            .embed("addressHistory",
              Address("Argonautenstraat", 3) ::
              Address("Overtoom", 15) :: Nil)
            .link("books", u"/customers/deadbeaf/books")

        customer.toJson must_== """
        {
          "email": "test@gmail.com",
          "_embedded": {
             "addressHistory": [{
                "street": "Argonautenstraat",
                "house": 3
             },{
                "street": "Overtoom",
                "house": 15
             }]
          },
          "_links": {
             "self": {"href": "/customers/deadbeaf"},
             "books": {"href": "/customers/deadbeaf/books"}
          }
        }
        """.parseJson
      }

      "multiple collection resources" >> {
        val customer =
          Resource(Customer("test@gmail.com"), u"/customers/deadbeaf")
            .embed("addressHistory",
              Resource(Address("Argonautenstraat", 3), r"/address/1") ::
              Resource(Address("Overtoom", 15), r"/address/2") :: Nil)
            .embed("cars",
              Resource(Car("BMW", "i3"), r"/cars/bmw_i3") ::
              Resource(Car("Nissan", "Leaf"), r"/cars/nissan_leaf") :: Nil)
            .link("books", r"/books")

        customer.toJson must_== """
        {
          "email": "test@gmail.com",
          "_embedded": {
             "addressHistory": [{
                "street": "Argonautenstraat",
                "house": 3,
                "_links": {
                   "self": {"href": "/customers/deadbeaf/address/1"}
                }
             },{
                "street": "Overtoom",
                "house": 15,
                "_links": {
                   "self": {"href": "/customers/deadbeaf/address/2"}
                }
             }],
             "cars": [{
                "make": "BMW",
                "model": "i3",
                "_links": {
                   "self": {"href": "/customers/deadbeaf/cars/bmw_i3"}
                }
             },{
                "make": "Nissan",
                "model": "Leaf",
                "_links": {
                   "self": {"href": "/customers/deadbeaf/cars/nissan_leaf"}
                }
             }]
          },
          "_links": {
             "self": {"href": "/customers/deadbeaf"},
             "books": {"href": "/customers/deadbeaf/books"}
          }
        }
        """.parseJson
      }

      "DRY resource links" >> {
        val customer =
          Resource(Customer("test@gmail.com"), u"/customers/deadbeaf")
            .link("tokens", r"/tokens")
            .embed("addressHistory",
              Resource(Address("Argonautenstraat", 3), r"/address/1")
                .link("business", r"/business")
                .embed("locations", Resource(Location(10.0, 12.0), r"/location")
                  .link("hide", r"/hide") :: Nil) :: Nil)

        customer.toJson must_== """
        {
          "email": "test@gmail.com",
          "_embedded": {
             "addressHistory": [{
                "street": "Argonautenstraat",
                "house": 3,
                "_embedded": {
                    "locations": [{
                       "lng": 10.0,
                       "lat": 12.0,
                       "_links": {
                           "self": { "href" : "/customers/deadbeaf/address/1/location"},
                           "hide": { "href" : "/customers/deadbeaf/address/1/location/hide"}
                       }
                     }]
                },
                "_links": {
                   "self": {"href": "/customers/deadbeaf/address/1"},
                   "business": {"href": "/customers/deadbeaf/address/1/business"}
                }
             }]
          },
          "_links": {
              "self":   {"href": "/customers/deadbeaf"},
              "tokens": {"href": "/customers/deadbeaf/tokens"}
          }
        }
        """.parseJson
      }

    }
  }
}
