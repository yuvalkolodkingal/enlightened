#!/usr/bin/env python3
"""Places the screenshots (docs/wiki/images) in the wiki's pages, English and Hebrew: under a page's language link, or
under a section's heading, each with its caption. Run once after adding a picture to the table; it skips a picture a
page already shows.

    python3 tools/wiki_images.py
"""
import os

WIKI = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'docs', 'wiki')

# page: [(anchor, image, English caption, Hebrew caption)]; the anchor is a heading ('## ...') or None for the top.
# For a Hebrew page the anchor is the Hebrew heading, the second of a pair 'English|Hebrew'.
PICTURES = {
    'Home': [(None, 'avatar-sphere', 'The Avatar in the elemental sphere', 'האווטאר בכדור היסודות'),
             (None, 'glider', 'Gliding on the Air Glider', 'גלישה על דאון האוויר'),
             (None, 'omashu', 'Omashu, King Bumi\'s city', 'אומאשו, העיר של המלך בומי'),
             (None, 'anomaly', 'The Anomaly of Seven Havens', 'האנומליה של שבעת המקלטים')],
    'Airbending': [(None, 'glider', 'The Air Glider, open in a glide', 'דאון האוויר, פתוח בגלישה'),
                   (None, 'glider-front', 'The bender hangs under the wings', 'הכשף תלוי מתחת לכנפיים'),
                   (None, 'tornado', 'The tornado', 'הטורנדו'),
                   (None, 'air-spout', 'The air spout', 'עמוד האוויר')],
    'Waterbending': [(None, 'frost-walk', 'Frost Walk: the water freezes under the feet', 'הליכת כפור: המים קופאים מתחת לרגליים'),
                     (None, 'octopus', 'The octopus form', 'צורת התמנון'),
                     (None, 'water-spout', 'The water spout', 'עמוד המים')],
    'Firebending': [(None, 'fire-fist', 'Fire in both fists', 'אש בשני האגרופים'),
                    (None, 'lightning', 'Lightning, drawn after Thor\'s beam', 'ברק, מצויר כמו קרן של ת\'ור'),
                    (None, 'rainbow-fire', 'Fire in its chosen colour: here, rainbow', 'אש בצבע שבחרתם: כאן, קשת')],
    'Earthbending': [(None, 'lavabending', 'Lavabending', 'כשפות לבה')],
    'The-Avatar': [(None, 'avatar-sphere', 'The elemental sphere', 'כדור היסודות'),
                   (None, 'avatar-state-part', 'A part of the Avatar State, in the first chakra\'s red', 'חלק ממצב האווטאר, באדום של הצ\'אקרה הראשונה')],
    'Chakras-and-the-Avatar-State': [
        ('## The Avatar State|## מצב האווטאר', 'avatar-state-part', 'One chakra open: a part of the State, wrapped in red', 'צ\'אקרה אחת פתוחה: חלק מהמצב, עטוף באדום'),
        ('## The Avatar State|## מצב האווטאר', 'avatar-state-fourth-chakra', 'Four open: the glow is the Air chakra\'s green', 'ארבע פתוחות: הזוהר בירוק של צ\'אקרת האוויר'),
        (None, 'guru-pathik', 'Guru Pathik teaches the chakras', 'גורו פאתיק מלמד את הצ\'אקרות'),
        (None, 'cosmic-plane', 'The cosmic plane of the seventh chakra', 'העולם הקוסמי של הצ\'אקרה השביעית'),
        (None, 'seven-chakras', 'The seven chakras at the end of the path of energy', 'שבע הצ\'אקרות בסוף שביל האנרגיה')],
    'Southern-Air-Temple': [(None, 'air-temple', 'The Southern Air Temple', 'מקדש האוויר הדרומי'),
                            (None, 'sky-bison', 'A sky bison, saddled and flown', 'ביזון שמיים, עם אוכף ובמעוף'),
                            (None, 'bison-flying', 'Flying over the temple', 'במעוף מעל המקדש'),
                            (None, 'guru-pathik', 'Guru Pathik in his meditation room', 'גורו פאתיק בחדר המדיטציה שלו')],
    'Lion-Turtle': [(None, 'lion-turtle', 'The lion turtle\'s head over the sea', 'ראש הצב-אריה מעל הים'),
                    (None, 'lion-turtle-gift', 'The gift of energybending', 'מתנת כיפוף האנרגיה')],
    'Flying-Lemur': [(None, 'momo', 'A tamed lemur rides on your shoulder', 'למור מאולף רוכב על הכתף')],
    'Looks': [(None, 'looks', 'Bumi, Iroh, Jeong Jeong, Pavi and Nisha', 'בומי, אירו, ג\'ונג ג\'ונג, פאבי ונישה')],
    'Earth-Kingdom-Cities': [
        ('## Ba Sing Se|## בה סינג סה', 'ba-sing-se', 'Ba Sing Se: three rings round the palace', 'בה סינג סה: שלוש טבעות סביב הארמון'),
        ('## Ba Sing Se|## בה סינג סה', 'ba-sing-se-wall', 'The Outer Wall', 'החומה החיצונית'),
        ('## Omashu|## אומאשו', 'omashu', 'Omashu over its chasm, the bridge on tall arches', 'אומאשו מעל התהום, הגשר על קשתות גבוהות'),
        ('## Omashu|## אומאשו', 'omashu-terraces', 'The terraces and the pyramid on the summit', 'הטרסות והפירמידה בפסגה'),
        ('## Omashu|## אומאשו', 'king-bumi', 'King Bumi on his throne', 'המלך בומי על כסאו'),
        ('## City guards|## שומרי העיר', 'city-guards', 'The guards of Omashu, Ba Sing Se and Ellora', 'השומרים של אומאשו, בה סינג סה ואלורה')],
    'Seven-Havens': [
        ('## The brigade armor trim|## עיטור השריון של הבריגדה', 'brigade-trim', 'The brigade armor trim', 'עיטור השריון של הבריגדה'),
        ('## The Anomaly|## האנומליה', 'anomaly', 'The Anomaly, woken from the red cubes', 'האנומליה, שהתעוררה מהקוביות האדומות'),
        ('## The Anomaly|## האנומליה', 'anomaly-day', 'Its red lightning by day', 'הברק האדום שלה ביום'),
        ('## The Anomaly|## האנומליה', 'seven-chakras', 'Beaten, it shows you the seven chakras', 'כשמנצחים אותה, היא מראה את שבע הצ\'אקרות'),
        ('## The Anomaly|## האנומליה', 'first-chakra', 'The first chakra, Muladhara: walk into it', 'הצ\'אקרה הראשונה, מולדהרה: היכנסו אליה'),
        ('## Ellora|## אלורה', 'ellora', 'Ellora, the golden city', 'אלורה, עיר הזהב')],
    'Spirit-World': [(None, 'spirit-world', 'The Spirit World', 'עולם הרוחות'),
                     (None, 'spirit-portal', 'A spirit portal in the forest', 'שער רוחות ביער')],
    'Items-and-Crafting': [(None, 'back-slot', 'Anything on your back: here, a sword', 'כל דבר על הגב: כאן, חרב'),
                           (None, 'back-slot-inventory', 'The back slot, beside the body', 'משבצת הגב, ליד הגוף')],
}


def place(page, hebrew):
    name = page + ('-HE' if hebrew else '') + '.md'
    path = os.path.join(WIKI, name)
    if not os.path.exists(path):
        return
    lines = open(path, encoding='utf-8').read().split('\n')
    groups = {}
    for anchor, image, en, he in PICTURES[page]:
        if not os.path.exists(os.path.join(WIKI, 'images', image + '.jpg')):
            continue
        if any('images/' + image + '.jpg' in l for l in lines):
            continue
        key = None if anchor is None else anchor.split('|')[1 if hebrew else 0]
        groups.setdefault(key, []).append((image, he if hebrew else en))
    for key, pics in groups.items():
        if key is None:
            # Under the language link near the top (or the title, if the page has none).
            at = next((i for i, l in enumerate(lines) if ('(Home' in l or '-HE)' in l or '](' + page + ')' in l) and ('[English]' in l or '[עברית]' in l)),
                      next(i for i, l in enumerate(lines) if l.startswith('# ')))
        else:
            at = next((i for i, l in enumerate(lines) if l.strip() == key), None)
            if at is None:
                continue
        block = ['']
        for image, caption in pics:
            block += ['![%s](images/%s.jpg)' % (caption, image), '*%s*' % caption, '']
        lines[at + 1:at + 1] = block
    open(path, 'w', encoding='utf-8').write('\n'.join(lines))


def main():
    for page in PICTURES:
        place(page, False)
        place(page, True)


if __name__ == '__main__':
    main()
