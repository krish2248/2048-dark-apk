// Names are public, so keep the worst out automatically; anything else goes through player reports.
const BLOCKED = [
  'fuck', 'shit', 'cunt', 'bitch', 'whore', 'slut', 'nigg', 'fagg', 'retard', 'rapist', 'porn', 'dick', 'cock',
  'pussy', 'penis', 'vagina', 'nazi', 'hitler', 'kike', 'chink', 'tranny', 'pedo', 'isis',
];
const RESERVED = ['admin', 'moderator', 'official', 'sonikrish'];
// short stems that appear inside ordinary names and places are only blocked as whole words
const WHOLE_WORD = new Set(['isis', 'cock', 'dick', 'pedo', 'chink']);

const LEET = { 0: 'o', 1: 'i', 3: 'e', 4: 'a', 5: 's', 7: 't', 8: 'b', 9: 'g' };

/** Returns the tidied name, or null if it isn't allowed. */
export function cleanName(raw) {
  if (typeof raw !== 'string') return null;
  const name = raw.normalize('NFKC').replace(/\s+/g, ' ').trim();
  if ([...name].length < 3 || [...name].length > 16) return null;
  if (!/^[\p{L}\p{M}\p{N} ._-]+$/u.test(name) || !/[\p{L}\p{N}]/u.test(name)) return null;
  const plain = name.toLowerCase().normalize('NFD').replace(/\p{M}/gu, '').replace(/[0-9]/g, (d) => LEET[d] ?? d);
  const squashed = plain.replace(/[^\p{L}]/gu, '');
  const words = plain.split(/[^\p{L}]+/u);
  if (RESERVED.some((w) => squashed.includes(w))) return null;
  if (BLOCKED.some((w) => (WHOLE_WORD.has(w) ? words.includes(w) || squashed === w : squashed.includes(w)))) return null;
  return name;
}

const COUNTRIES = new Set((
  'AD AE AF AG AI AL AM AO AQ AR AS AT AU AW AX AZ BA BB BD BE BF BG BH BI BJ BL BM BN BO BQ BR BS BT BV BW BY BZ ' +
  'CA CC CD CF CG CH CI CK CL CM CN CO CR CU CV CW CX CY CZ DE DJ DK DM DO DZ EC EE EG EH ER ES ET FI FJ FK FM FO FR ' +
  'GA GB GD GE GF GG GH GI GL GM GN GP GQ GR GS GT GU GW GY HK HM HN HR HT HU ID IE IL IM IN IO IQ IR IS IT JE JM JO JP ' +
  'KE KG KH KI KM KN KP KR KW KY KZ LA LB LC LI LK LR LS LT LU LV LY MA MC MD ME MF MG MH MK ML MM MN MO MP MQ MR MS MT ' +
  'MU MV MW MX MY MZ NA NC NE NF NG NI NL NO NP NR NU NZ OM PA PE PF PG PH PK PL PM PN PR PS PT PW PY QA RE RO RS RU RW ' +
  'SA SB SC SD SE SG SH SI SJ SK SL SM SN SO SR SS ST SV SX SY SZ TC TD TF TG TH TJ TK TL TM TN TO TR TT TV TW TZ UA UG ' +
  'UM US UY UZ VA VC VE VG VI VN VU WF WS XK YE YT ZA ZM ZW'
).split(' '));

export const isCountry = (c) => typeof c === 'string' && COUNTRIES.has(c);
