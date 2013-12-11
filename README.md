# GMailToLabel

A little Clojure+Swing app to fetch a group of your contacts in GMail and conjure up mailing labels out of them
with Java2D printing.

## Usage

To start, download and double-click or type "java -jar gmailtolabel-1.0-standalone.jar" in the command line. Java needs
to be installed.

Type your GMail username and password, and "Get groups" to download a list of groups. You can create one in GMail to
select your contacts for mailing.

Select the correct group from dropdown. If you want, you may type below a custom name field. (in Google Contacts, you can
add custom fields to contacts; this would be useful if you want e.g. "Foo + family" to appear as a name instead of just
"Foo Bar" if those were first name and surname.)

Click "Get Addresses" to see a preview of how your labels would look. Now measure your labels and tweak the sizes if needed;
click "Update" to see your changes.

When done, click "Print". In the following dialog you can select the margin sizes for perfect alignment. Try with plain
paper first before wasting the sticker paper!

## Development

GMailToLabel uses [Google Contacts API](https://developers.google.com/google-apps/contacts/v3/) and
[Seesaw GUI library](https://github.com/daveray/seesaw) to get things done.

 
## License

Distributed under the Eclipse Public License, the same as Clojure.


