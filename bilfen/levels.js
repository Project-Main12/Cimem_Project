/* Gün Dönümü — 10 bölüm. Harita: 30 sütun × 17 satır, her kare 32 px.
   .  boş        #  zemin        -  ince platform (alttan geçilir)
   X  tehlike (bataklık / su / lav / buzlu su: dokununca başa dönersin)
   M  sağa sola giden platform   N  aşağı yukarı giden platform
   C  çöken platform (basınca kısa süre sonra düşer, sonra geri gelir)
   J  gayzer (yüksek zıplatır)   I  buz (kaygan)
   E  yürüyen Tutulma Gölgesi   F  uçan Tutulma Gölgesi (değersen başa dönersin)
   g  sikke (hepsi toplanmadan kapı açılmaz)   P  başlangıç   K  kapı */
window.LEVELS = [
{
  name: "Ekvator", place: "Amazon Yağmur Ormanı", theme: "forest", target: 35, haz: "Bataklık",
  fact: "Ekvator'da yıl boyunca gece ve gündüz yaklaşık 12'şer saattir. 21 Aralık'ta da durum aynıdır.",
  tip: "Bütün sikkeleri topla. Sikkelerin hepsi toplanmadan kapı açılmaz.",
  map: [
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "...............g.g............",
    "...............------.........",
    "..............................",
    "..........g.g...........g.g...",
    ".........-----..........-----.",
    "..............................",
    "....g.g.......................",
    "...-----......................",
    ".P.........g.g..............K.",
    "##############################",
    "##############################"
  ]
},
{
  name: "Yengeç Dönencesi", place: "Sahra Çölü", theme: "desert", target: 40, haz: "Kum bataklığı",
  fact: "Yengeç Dönencesi 23°27′ Kuzey enlemidir. 21 Aralık'ta buradaki öğle güneşi yılın en alçak açısıyla gelir.",
  tip: "Yeni düşman: Tutulma Gölgesi. Ona dokunursan başa dönersin, zamanlamanı ayarlayıp üstünden atla. Kum bataklıklarına da basma.",
  map: [
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "....................g.g.......",
    "...................-----......",
    "...........g..................",
    "..........---......g..........",
    ".....g.g..........---.........",
    "....-----.....................",
    ".P.........g.E....g.......g.K.",
    "#######XX#######XX#####XXX####",
    "##############################"
  ]
},
{
  name: "Oğlak Dönencesi", place: "Avustralya Çölleri", theme: "outback", target: 45, haz: "Bataklık",
  fact: "21 Aralık'ta öğle vakti güneş ışınları Oğlak Dönencesi'ne (23°27′ Güney) dik gelir. Burada cisimlerin gölgesi kaybolur.",
  tip: "Yeni: sağa sola giden platform. Üstünde durursan seni de taşır.",
  map: [
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "...........g.g................",
    "..........-----...............",
    "....................g.g.......",
    "..........g.......-----.......",
    "...---...MMM..................",
    ".P.g.............Eg.......g.K.",
    "######XXXXXXXXXX#####XXX######",
    "##############################"
  ]
},
{
  name: "Akdeniz İklimi", place: "İskenderun Körfezi", theme: "sea", target: 55, haz: "Derin su",
  fact: "İskenderun yaklaşık 36,6° Kuzey enlemindedir. 21 Aralık'ta burada gündüz yaklaşık 9 saat 40 dakika, gece 14 saat 20 dakika sürer.",
  tip: "Yeni: aşağı yukarı giden platform ve uçan Tutulma Gölgesi. Platform alçaldığında üstüne atla, seni yukarı taşısın.",
  map: [
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............ggg.............",
    ".............-----............",
    "..............................",
    ".........................F....",
    "..........NN.........g.g......",
    "....................-----.....",
    "..............................",
    "....ggg...........MMM.........",
    "...-----......................",
    ".P....................Eg....K.",
    "#########XXXXXXXXXXX#####XX###",
    "##############################"
  ]
},
{
  name: "Karst Mağarası", place: "Toros Dağları", theme: "cave", target: 55, haz: "Yeraltı suyu",
  fact: "Kuzey Yarımküre'de 21 Aralık'ta gündüz en kısadır. Mağaranın dışında güneş bugün erken batacak.",
  tip: "Yeni: çatlak taşlar. Üstüne basınca kısa süre sonra çöker, oyalanma!",
  map: [
    "##############################",
    "##############################",
    "##..####...########...####..##",
    "..............................",
    "..............................",
    "..............................",
    "................F.............",
    "..............................",
    "..........ggg.................",
    ".........CCCCC................",
    ".................g...g.g......",
    "....g.g.........CCC.-----.....",
    "...-----......................",
    "..............................",
    ".P.......................gE.K.",
    "########XXXXXXXXXXXX##XXX#####",
    "##############################"
  ]
},
{
  name: "Volkanik Dağlar", place: "Nemrut Kalderası", theme: "volcano", target: 55, haz: "Lav",
  fact: "Kış gündönümünde Kuzey Yarımküre'deki dağlarda gece en uzundur. Uzun gece, yüksek yerlerde soğuğu artırır.",
  tip: "Yeni: gayzer. Gri taşın üstüne basınca seni çok yükseğe fırlatır.",
  map: [
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "................g.g...........",
    "...............CCCCC..........",
    "..............................",
    "..........g.g.................",
    ".........-----......MMM.......",
    "..............................",
    "..............F...............",
    "....g.g...................g...",
    "...CCCCC............---..---..",
    "..............................",
    ".P.......g........g........EK.",
    "########J#XXXXXXX##XXXXXX#####",
    "##############################"
  ]
},
{
  name: "Delta Ovası", place: "Çukurova (Seyhan ve Ceyhan)", theme: "delta", target: 50, haz: "Nehir",
  fact: "21 Aralık'tan sonra Kuzey Yarımküre'de gündüzler her gün biraz uzamaya başlar.",
  tip: "Nehir kollarının arasındaki adalardan atlayarak ilerle. Geniş kolu hareketli platformla geç.",
  map: [
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "............F.................",
    "..............................",
    "...........g.g......g.g.......",
    "..........CCCC.....----.......",
    "................MMM...........",
    ".P.g....Eg....g.........g...K.",
    "####XXX###XXX###XXXXXXX###XX##",
    "##############################"
  ]
},
{
  name: "Kuzey Kutup Dairesi", place: "Kutup Gecesi", theme: "polarnight", target: 60, haz: "Buzlu su",
  fact: "21 Aralık'ta Kuzey Kutup Dairesi'nden kutba doğru güneş hiç doğmaz. 24 saat gece yaşanır.",
  tip: "Yeni: buz. Buzun üstünde kayarsın, durmak zorlaşır.",
  map: [
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "..............................",
    "...............g.g............",
    "..............CCCCC...........",
    ".....................F........",
    "...........NN.................",
    "..............................",
    ".....ggg................g.g...",
    "....IIIII..............IIII...",
    "..............................",
    ".P..........gE......g.......K.",
    "###IIIXXXXXIIIIXXXXIIIXXXIII##",
    "##############################"
  ]
},
{
  name: "Güney Kutup Dairesi", place: "Antarktika", theme: "polarday", target: 65, haz: "Buzlu su",
  fact: "21 Aralık'ta Güney Kutup Dairesi'nden kutba doğru güneş hiç batmaz. 24 saat gündüz yaşanır. Güney Yarımküre'de yaz başlar.",
  tip: "Uzun bir buzlu su göleti. Çöken taşlar ve hareketli platformlarla öbür tarafa geç.",
  map: [
    "..............................",
    "..............................",
    "..........F...................",
    "......................g.g.....",
    ".....................CCCCC....",
    "..............................",
    ".............MMM..............",
    "..................F...........",
    "........gg....................",
    "........CCC...........NN......",
    "..............................",
    "....g.g.......................",
    "...IIIII......................",
    "..............................",
    ".P.g....................g.E.K.",
    "####IIIXXXXXXXXXXXXXXXXIII####",
    "##############################"
  ]
},
{
  name: "Gündönümü", place: "Dünya'nın Ekseni (23°27′)", theme: "solstice", target: 70, haz: "Lav",
  fact: "Dünya'nın ekseni yörünge düzlemine göre 23°27′ eğiktir. Mevsimleri ve 21 Aralık gündönümünü bu eğiklik oluşturur.",
  tip: "Son bölüm: gayzer, çöken taşlar, iki çeşit hareketli platform ve buz bir arada.",
  map: [
    "..............................",
    "..............................",
    "..............................",
    "..................F...........",
    ".........g.g..................",
    "........CCCCC...........ggg...",
    ".......................-----..",
    "...g.g........................",
    "..-----.......................",
    "...........MMM........NN......",
    "..............................",
    "......F.......................",
    ".........................F....",
    "..............................",
    ".P.g..............gE........K.",
    "#####J##XXXXXXXXXXIII#XXXXIIII",
    "##############################"
  ]
}
];
