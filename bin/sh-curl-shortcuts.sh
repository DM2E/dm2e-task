alias curl="eval /usr/bin/curl -v"
HEAD="-X HEAD"
GET="-X GET"
POST="-X POST"
PUT="-X PUT"
DELETE="-X DELETE"

CT_TEXT='-H "Content-Type: text/plain"'
AC_TEXT='-H "Accept: text/plain"'

CT_TURTLE='-H "Content-Type: text/turtle"'
AC_TURTLE='-H "Accept: text/turtle"'

CT_NTRIPLES='-H "Content-Type: application/rdf-triples"'
AC_NTRIPLES='-H "Accept: application/rdf-triples"'

CT_N3='-H "Content-Type: text/rdf+n3"'
AC_N3='-H "Accept: text/rdf+n3"'

CT_RDFXML='-H "Content-Type: application/rdf+xml"'
AC_RDFXML='-H "Accept: application/rdf+xml"'
